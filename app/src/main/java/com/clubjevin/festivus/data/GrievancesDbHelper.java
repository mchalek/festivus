package com.clubjevin.festivus.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by kevin on 12/1/16.
 */

public class GrievancesDbHelper extends SQLiteOpenHelper {
    // Mostly copied from https://developer.android.com/training/basics/data-storage/databases.html#DbHelper
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "FestivusGrievances.db";
    public static final String TABLE_NAME = "grievances";

    public static class ColumnNames {
        public static final String TIMESTAMP = "timestamp";
        public static final String GRIEVANCE = "grievance";
    }

    public static String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " ( " + ColumnNames.TIMESTAMP + " BIGINT, " + ColumnNames.GRIEVANCE + " TEXT )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public GrievancesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
