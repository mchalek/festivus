package com.clubjevin.festivus.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by kevin on 12/1/16.
 */

public class GrievancesDAO {
    private SQLiteDatabase db;
    private final Random rng;

    public GrievancesDAO(Context context) {
        GrievancesDbHelper dbHelper = new GrievancesDbHelper(context);
        db = dbHelper.getWritableDatabase();
        rng = new Random();
    }

    public void close() {
        if(db != null) {
            db.close();
        }
        db = null;
    }

    public void insert(Grievance grievance) {
        ContentValues values = new ContentValues();
        values.put(GrievancesDbHelper.ColumnNames.TIMESTAMP, grievance.getTimestamp());

        if(grievance.getText() != null) {
            values.put(GrievancesDbHelper.ColumnNames.GRIEVANCE_TEXT, grievance.getText());
        }

        if(grievance.getRecording() != null) {
            values.put(GrievancesDbHelper.ColumnNames.GRIEVANCE_RECORDING, grievance.getRecording());
        }

        synchronized(db) {
            db.insert(GrievancesDbHelper.TABLE_NAME, null, values);
        }
    }

    public List<Grievance> read() {
        String[] columnNames = {
                GrievancesDbHelper.ColumnNames.TIMESTAMP,
                GrievancesDbHelper.ColumnNames.GRIEVANCE_TEXT,
                GrievancesDbHelper.ColumnNames.GRIEVANCE_RECORDING
        };

        Cursor cursor = null;
        synchronized(db) {
            cursor = db.query(GrievancesDbHelper.TABLE_NAME, columnNames, null, null, null, null, null);
        }

        List<Grievance> result = new ArrayList<>();

        boolean rowsRemain = cursor.moveToFirst();
        while(rowsRemain) {
            Grievance grievance = null;
            if(cursor.isNull(1) && !cursor.isNull(2)) {
                grievance = new Grievance(cursor.getLong(0), cursor.getString(1), null);
            } else if(!cursor.isNull(1) && cursor.isNull(2)) {
                grievance = new Grievance(cursor.getLong(0), null, cursor.getString(2));
            } else if(!cursor.isNull(1) && !cursor.isNull(2)) {
                throw new IllegalArgumentException("Invalid database row: cannot populate both text and recording path!");
            } else {
                throw new IllegalArgumentException("Invalid database row: text and recording path cannot both be null!");
            }
            result.add(grievance);

            rowsRemain = cursor.moveToNext();
        }

        return result;
    }

    public Grievance readRandom() {
        List<Grievance> allGrievances = read();
        if(allGrievances.size() == 0) {
            return null;
        }

        Integer index = rng.nextInt(allGrievances.size());
        return allGrievances.get(index);
    }
}
