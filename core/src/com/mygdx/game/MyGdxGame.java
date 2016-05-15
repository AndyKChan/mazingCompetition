package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

//import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Sprites.Starship;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MyGdxGame extends ApplicationAdapter {
	private final float UPDATE_TIME = 1/60f;
	float timer;
	SpriteBatch batch;
	private Socket socket;
	String id;
	Starship player;
	Texture playerShip;
	Texture friendlyShip;
	HashMap<String, Starship> friendlyPlayers;

	@Override
	public void create () {
		batch = new SpriteBatch();
		playerShip = new Texture("playerShip2.png");
		friendlyShip = new Texture("playerShip.png");
		friendlyPlayers = new HashMap<String, Starship>();
		connectSocket();
		configSocketEvents();
	}

	public void handleInput(float dt){
		if(player != null){
			if(Gdx.input.isKeyPressed(Input.Keys.LEFT)){
				player.setPosition(player.getX() + (-200 * dt), player.getY());
			} else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
				player.setPosition(player.getX() + (+200 * dt), player.getY());
			} else if(Gdx.input.isKeyPressed(Input.Keys.UP)){
				player.setPosition(player.getX(), player.getY() + (+200 * dt));
			} else if(Gdx.input.isKeyPressed(Input.Keys.DOWN)){
				player.setPosition(player.getX(), player.getY() + (-200 * dt));
			}
		}
	}

	public void updateServer(float dt){
		timer += dt;
		if(timer >= UPDATE_TIME && player != null && player.hasMoved()){
			JSONObject data = new JSONObject();
			try{
				data.put("x", player.getX());
				data.put("y", player.getY());
				socket.emit("playerMoved", data);
			} catch(JSONException e){
				Gdx.app.log("SOCKET.IO", "Error sending update data");
			}
		}
	}

	@Override
	public void render () {
		handleInput(Gdx.graphics.getDeltaTime());
		updateServer(Gdx.graphics.getDeltaTime());
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		if(player != null){
			player.draw(batch);
		}


		friendlyPlayers.forEach((k,v) -> v.draw(batch));

		batch.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		playerShip.dispose();
		friendlyShip.dispose();
	}

	public void connectSocket(){
		try {
			socket = IO.socket("http://localhost:8080");
			socket.connect();
		} catch(Exception e){
			System.out.println(e);
		}
	}

	public void configSocketEvents(){
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				Gdx.app.log("SocketIO", "Connected");
				player = new Starship(playerShip);
			}
		}).on("socketID", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {

					id = data.getString("id");
					Gdx.app.log("SocketIO", "My ID: " + id);
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting ID");
				}
			}
		}).on("newPlayer", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String playerID = data.getString("id");
					Gdx.app.log("SocketIO", "New Player Connect: " + id);
					friendlyPlayers.put(playerID, new Starship(friendlyShip));
				}catch(JSONException e){
					Gdx.app.log("SocketIO", "Error getting New PlayerID");
				}
			}
		}).on("playerDisconnected", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					id = data.getString("id");
					friendlyPlayers.remove(id);
				}catch(JSONException e){
					Gdx.app.log("SocketIO", "Error getting disconnected PlayerID");
				}
			}
		}).on("playerMoved", new Emitter.Listener() {
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String playerID = data.getString("id");
					Double x = data.getDouble("x");
					Double y = data.getDouble("y");
					if(friendlyPlayers.get(playerID) != null){
						friendlyPlayers.get(playerID).setPosition(x.floatValue(), y.floatValue());
					}
				}catch(JSONException e){
					Gdx.app.log("SocketIO", "Error getting disconnected PlayerID");
				}
			}
		}).on("getPlayers", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONArray objects = (JSONArray) args[0];
				try {
					for(int i = 0; i < objects.length(); i++){
						Starship coopPlayer = new Starship(friendlyShip);
						Vector2 position = new Vector2();
						position.x = ((Double) objects.getJSONObject(i).getDouble("x")).floatValue();
						position.y = ((Double) objects.getJSONObject(i).getDouble("y")).floatValue();
						coopPlayer.setPosition(position.x, position.y);

						friendlyPlayers.put(objects.getJSONObject(i).getString("id"), coopPlayer);
					}
				} catch(JSONException e){

				}
			}
		});
	}
}