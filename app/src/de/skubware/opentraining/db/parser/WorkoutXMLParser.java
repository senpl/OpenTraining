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

package de.skubware.opentraining.db.parser;

import android.content.Context;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.skubware.opentraining.Exceptions.ErrorException;
import de.skubware.opentraining.basic.ExerciseType;
import de.skubware.opentraining.basic.ExerciseType.ExerciseSource;
import de.skubware.opentraining.basic.FSet;
import de.skubware.opentraining.basic.FSet.SetParameter;
import de.skubware.opentraining.basic.FitnessExercise;
import de.skubware.opentraining.basic.TrainingEntry;
import de.skubware.opentraining.basic.Workout;
import de.skubware.opentraining.db.DataProvider;
import de.skubware.opentraining.db.IDataProvider;

/**
 * An implementation of a SaxParser for parsing .xml files to a {@link Workout}
 * object.
 * 
 * @author Christian Skubich
 */
public class WorkoutXMLParser extends DefaultHandler {
	/**
	 * Tag for logging
	 */
	static final String TAG = "WorkoutXMLParser";

	private Context mContext;

	private SAXParser mParser = null;

	private Workout mWorkout;

	// Workout stuff
	/**
	 * Name of the {@link Workout}
	 */
	private String mWorkoutName;

	/**
	 * Integer for the number of rows of the workout.
	 */
	private Integer mRowCount;

	// FitnessExercise stuff
	/**
	 * List to store the {@link FitnessExercise}s
	 */
	private List<FitnessExercise> mFExList = new ArrayList<FitnessExercise>();

	/**
	 * The last {@link ExerciseType} that was parsed
	 */
	private ExerciseType mExerciseType;

	/**
	 * The custom name of the {@link FitnessExercise}
	 */
	private String mCustomName;

	// TrainingEntry stuff
	/**
	 * A Map for the TrainingEntrys of a FitnessExercise.
	 */
	private List<TrainingEntry> mTrainingEntryList = new ArrayList<TrainingEntry>();

	/**
	 * Date for the last parsed {@link TrainingEntry}
	 */
	private TrainingEntry mTrainingEntry;

	// FSet stuff
	/**
	 * List for the {@link FSet}s
	 */
	private List<FSet> mFSetList = new ArrayList<FSet>();

	/**
	 * List for the {@link FSet}s of the TrainingEntry
	 */
	private List<FSet> mTrainingEntryFSetList = new ArrayList<FSet>();

	/**
	 * Map for the status of the FSets.
	 */
	private Map<FSet, Boolean> mSetHasBeenDoneMap = new HashMap<FSet, Boolean>();

	/**
	 * The status of the FSet
	 */
	private boolean mSetHasBeenDone = true;

	private boolean parsingTrainingEntry = false;

	/**
	 * List of the {@link SetParameter}s
	 */
	private List<SetParameter> mSetParameter = new ArrayList<SetParameter>();

	/**
	 * Name of the last parsed {@link SetParameter}
	 */
	private String mSetParameterName;

	/**
	 * Value of the last parsed {@link SetParameter}
	 */
	private String mSetParameterValue;

	public WorkoutXMLParser() {
		// IParser instanziieren
		try {
			SAXParserFactory fac = SAXParserFactory.newInstance();
			mParser = fac.newSAXParser();
		} catch (Exception e) {
			Log.v("WorkoutXMLParser", e.getMessage().toString());
		}

	}

	public Workout read(File f, Context context) {
		mContext = context;

		try {
			// Dokument parsen
			mParser.parse(f, this);

			return this.mWorkout;
		} catch (SAXException e) {
			String workoutString = "";
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line;
				StringBuilder sb = new StringBuilder();

				while ((line = br.readLine()) != null) {
					sb.append(line.trim());
				}
				workoutString = sb.toString();
			} catch (IOException ioEx) {
				Log.e(TAG, "Error during reading Workout file.", ioEx);
			}

			Log.e(TAG, "Error during parsing Workout. Workout file: \n " + workoutString, e);
		} catch (Exception e) {
			Log.e(TAG, "Error during parsing Workout.", e);
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	// because using constructor of TrainingEntry
	@Override
	public void startElement(String uri, String name, String qname, Attributes attributes) throws SAXException {
		switch (qname) {
			case "Workout":
				workOutActionsStart(attributes);
				break;
			case "FitnessExcercise":
				this.mCustomName = attributes.getValue("customname");
				break;
			case "ExcerciseType":
				try {
					exerciseTypeActionsStart(attributes);
				} catch (ErrorException e) {
					Log.v("WorkoutXMLParser", e.getMessage());
				}
				break;
			case "Fset":
				fSetActionsStart(attributes);
				break;
			case "SetParameter":
				this.mSetParameterName = attributes.getValue("name");
				this.mSetParameterValue = attributes.getValue("value");
				break;
			case "TrainingEntry":
				trainingEntryActionsStart(attributes);
				break;
			default:
				break;
		}
	}

	private void exerciseTypeActionsStart(Attributes attributes) throws ErrorException {
		String exName = attributes.getValue("name");
		IDataProvider dataProvider = new DataProvider(mContext);
		this.mExerciseType = dataProvider.getExerciseByName(exName);

		// if exercise can't be found, create and save it
		// this may happen if a custom(or synced) exercise has been deleted
		if (mExerciseType == null) {
            Log.e(TAG, "Could not find exercise, will create new custom exercise with the name " + exName, new NullPointerException("The exercise '" + exName + "' of the TrainingPlan couldn't be found in the database."));
            mExerciseType = (new ExerciseType.Builder(exName, ExerciseSource.CUSTOM)).build();
            dataProvider.saveCustomExercise(mExerciseType);
        }
	}

	private void workOutActionsStart(Attributes attributes) {
		mWorkoutName = attributes.getValue("name");
		String r = attributes.getValue("rows");
		if (r != null) {
            this.mRowCount = Integer.parseInt(r);
        }
	}

	private void trainingEntryActionsStart(Attributes attributes) {
		parsingTrainingEntry = true;
		String dateString = attributes.getValue("date");

		Date trainingEntryDate;
		if (dateString == null || dateString.equals("") || dateString.equals("null")) {
            trainingEntryDate = null;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

            try {
                trainingEntryDate = format.parse(dateString);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + dateString, e);
                trainingEntryDate = null;
            }
        }
		this.mTrainingEntry = new TrainingEntry(trainingEntryDate);
	}

	private void fSetActionsStart(Attributes attributes) {
		if (attributes.getValue("hasBeenDone") != null)
            mSetHasBeenDone = Boolean.parseBoolean(attributes.getValue("hasBeenDone"));
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		switch (qName) {
			case "Workout":
                workoutActionEnd();
				break;
			case "FitnessExercise":
                fitnessExerciseEnd();
				break;
			case "Fset":
                try {
                    fSetActionsEnd();
                } catch (ErrorException e) {
                    Log.v("WorkoutXMLParser", e.getMessage().toString());
                }
                break;
			case "SetParameter":
                setParamenterActionsEnd();
				break;
			case "TrainingEntry":
                trainingEntryActionsEnd();
                break;
			default:
				break;
		}
	}

    private void trainingEntryActionsEnd() {
        for (FSet set : this.mTrainingEntryFSetList) {
            try {
                mTrainingEntry.add(set);
            } catch (ErrorException e) {
                Log.v("WorkoutXMLParser", e.getMessage());
            }
            mTrainingEntry.setHasBeenDone(set, mSetHasBeenDoneMap.get(set));
        }

        this.mTrainingEntryList.add(this.mTrainingEntry);
        this.mTrainingEntry = null;
        this.mTrainingEntryFSetList = new ArrayList<FSet>();
        this.mSetHasBeenDoneMap = new HashMap<FSet, Boolean>();
        parsingTrainingEntry = false;
    }

    private void setParamenterActionsEnd() {
        boolean created = setParameterName();
        if(!created) {
            throw new IllegalStateException();
        }

        mSetParameterName = null;
        mSetParameterValue = null;
    }

    private void fSetActionsEnd() throws ErrorException {
        FSet createdFSet = new FSet(this.mSetParameter.toArray(new SetParameter[1]));
        if (parsingTrainingEntry && !mSetParameter.isEmpty()) {
            mTrainingEntryFSetList.add(createdFSet);
        } else if (!mSetParameter.isEmpty()) {
            mFSetList.add(createdFSet);
        }

        this.mSetHasBeenDoneMap.put(createdFSet, mSetHasBeenDone);
        this.mSetHasBeenDone = true;
        this.mSetParameter = new ArrayList<SetParameter>();
    }

    private void fitnessExerciseEnd()  {
        FitnessExercise fEx = null;
        try {
            fEx = new FitnessExercise(this.mExerciseType, this.mFSetList.toArray(new FSet[0]));
        } catch (ErrorException e) {
            Log.v("WorkoutXMLParser", e.getMessage());
        }

        // set custom name
        if (this.mCustomName != null) {
            try {
                fEx.setCustomName(mCustomName);
            } catch (ErrorException e) {
                Log.v("WorkoutXMLParser", e.getMessage());
            }
            Log.d(TAG, "customName=" + mCustomName);
        } else {
            Log.d(TAG, "No customName");
        }

        // now add TrainingEntrys
        List<TrainingEntry> originalList = fEx.getTrainingEntryList();
        originalList.addAll(this.mTrainingEntryList);

        this.mFExList.add(fEx);

        this.mCustomName = null;
        this.mExerciseType = null;
        this.mFSetList = new ArrayList<FSet>();
        this.mTrainingEntryList = new ArrayList<TrainingEntry>();
    }

    private void workoutActionEnd() {
        try {
            this.mWorkout = new Workout(this.mWorkoutName, this.mFExList.toArray(new FitnessExercise[0]));
        } catch (ErrorException e) {
            Log.v("WorkoutXMLParser", e.getMessage());
        }
        if (this.mRowCount != null) {
            this.mWorkout.setEmptyRows(this.mRowCount);
        } else {
            Log.d(TAG, "No rows were set");
        }

		this.mRowCount = null;
		this.mWorkoutName = null;
	}

	private boolean setParameterName() {
		boolean created = false;

		if (this.mSetParameterName.equals(new SetParameter.Weight(1).getName())) {
			this.mSetParameter.add(new SetParameter.Weight(Integer.parseInt(this.mSetParameterValue)));
			created = true;
		} else if (this.mSetParameterName.equals(new SetParameter.Repetition(1).getName())) {
			this.mSetParameter.add(new SetParameter.Repetition(Integer.parseInt(this.mSetParameterValue)));
			created = true;
		} else if (this.mSetParameterName.equals(new SetParameter.Duration(1).getName())) {
			this.mSetParameter.add(new SetParameter.Duration(Integer.parseInt(this.mSetParameterValue)));
			created = true;
		} else if (this.mSetParameterName.equals(new SetParameter.FreeField(" ").getName())) {
			this.mSetParameter.add(new SetParameter.FreeField(this.mSetParameterValue));
			created = true;
		}

		return created;
	}
}
