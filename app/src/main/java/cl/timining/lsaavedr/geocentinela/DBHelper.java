package cl.timining.lsaavedr.geocentinela;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static java.lang.Integer.toHexString;

public class DBHelper extends SQLiteOpenHelper
{
    private static final String TAG = "DBHelper";

    private static final int DB_VERSION = 3;
    private static final String DB_NAME = "geocentinela";

    private static final String DB_CREATE_TABLE_INSTRUMENTLIST =
            "create table instrumentlist (id integer," +
                    "name text," +
                    "hid integer, hmid integer, lmid integer, lid integer)";

    private static final String DB_CREATE_TABLE_FILELIST =
            "create table filelist (id integer primary key," +
                    "instrument_id integer default 0," +
                    "name text, size integer default 0, ts integer default 0, status integer default 0)";

    public DBHelper(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(DB_CREATE_TABLE_INSTRUMENTLIST);
        db.execSQL(DB_CREATE_TABLE_FILELIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int ov, int nv)
    {
        db.execSQL("DROP TABLE IF EXISTS instrumentlist;");
        db.execSQL("DROP TABLE IF EXISTS filelist;");

        db.execSQL(DB_CREATE_TABLE_INSTRUMENTLIST);
        db.execSQL(DB_CREATE_TABLE_FILELIST);
    }

    public int addInstrument(int id, int hid, int hmid, int lmid, int lid)
    {
        SQLiteDatabase dbw = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id", id);

        String name = toHexString(hid) + "." +
                toHexString(hmid) + "." +
                toHexString(lmid) + "." +
                toHexString(lid);

        values.put("name", name);
        values.put("hid", hid);
        values.put("hmid", hmid);
        values.put("lmid", lmid);
        values.put("lid", lid);

        int update = dbw.update("instrumentlist", values, "hid=? and hmid=? and lmid=? and lid=?",
                new String[] { ""+hid, ""+hmid, ""+lmid, ""+lid });
        if (update == 0) dbw.replace("instrumentlist", null, values);

        return update;
    }

    public int addFile(int instrument_id, String name, int size, int ts)
    {
        SQLiteDatabase dbw = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("instrument_id", instrument_id);
        values.put("name", name);
        values.put("size", size);
        values.put("ts", ts);

        int update = dbw.update("filelist", values, "instrument_id=? and name=?",
                new String[] { ""+instrument_id, name });
        if (update == 0) dbw.replace("filelist", null, values);

        return update;
    }

    public void rmFile(int instrument_id, String name)
    {
        SQLiteDatabase dbw = getWritableDatabase();

        dbw.delete("filelist", "instrument_id=? and name=?",
                new String[] { String.valueOf(instrument_id), name });
    }

    public void rmFile(int instrument_id)
    {
        SQLiteDatabase dbw = getWritableDatabase();

        dbw.delete("filelist", "instrument_id=?", new String[] { String.valueOf(instrument_id) });
    }

    public Cursor getInstrumentsCursor()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor find = db.query("instrumentlist",
                new String[] { "id as _id", "hid", "hmid", "lmid", "lid" },
                null, null,
                null, null,
                "id desc");
        return find;
    }

    public String getInstrumentName(int instrument_id)
    {
        String name = "";

        SQLiteDatabase db = getReadableDatabase();
        Cursor find = db.query("instrumentlist",
                new String[] { "name" },
                null, null,
                null, null,
                "id desc");
        if (find.moveToFirst() && !find.isAfterLast()) {
            do {
                name = find.getString(find.getColumnIndex("name"));
                break;
            } while (find.moveToNext());
        } find.close();

        return name;
    }

    public Cursor getFilesCursor(int instrument_id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor find = db.query("filelist",
                new String[] { "id as _id", "instrument_id", "name", "size", "ts", "status" },
                "instrument_id=?", new String[]{ ""+instrument_id },
                null, null,
                "name desc");
        return find;
    }

    public int updateStatus(int instrument_id, String filename, int status)
    {
        SQLiteDatabase dbw = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("status", status);

        int update = dbw.update("filelist", values, "instrument_id=? and name=?", new String[] { ""+instrument_id, filename });
        return update;
    }

    public int getStatus(int instrument_id, String filename)
    {
        int status = -1;
        SQLiteDatabase db = getReadableDatabase();
        Cursor find = db.query("filelist",
                new String[] { "status" },
                "instrument_id=? and name=?", new String[] { ""+instrument_id, filename },
                null, null,
                "id desc");
 
        if (find.moveToFirst() && !find.isAfterLast()) {
            do {
                status = find.getInt(find.getColumnIndex("status"));
            } while (find.moveToNext());
        } find.close();

        return status;
    }
}