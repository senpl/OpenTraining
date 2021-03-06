/**
 *
 * This is OpenTraining, an Android application for planning your your fitness training.
 * Copyright (C) 2012-2014 Christian Skubich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.skubware.opentraining.db.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * @class MuscleGSONDeserializer
 */
public class MuscleGSONDeserializer implements JsonDeserializer<ServerModel.MuscleCategory[]>{

	@Override
	public ServerModel.MuscleCategory[] deserialize(JsonElement json, Type type, JsonDeserializationContext jsonContext) throws JsonParseException {
		JsonArray muscleJsonArray = json.getAsJsonObject().get("results").getAsJsonArray();
		return new Gson().fromJson(muscleJsonArray, ServerModel.MuscleCategory[].class);
	}

}