/* *************************************************************************************************
 * IBMatcherDatabase.java
 *
 * DESCRIPTION:
 *     Example database for IBScanMatcher
 *     http://www.integratedbiometrics.com
 *
 * NOTES:
 *     Copyright (c) Integrated Biometrics, 2013
 *     
 * HISTORY:
 *     2013/03/22  First version.
 ************************************************************************************************ */

package com.facerec.tasol.tasolbiometricdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.integratedbiometrics.ibscanmatcher.IBMatcher;
import com.integratedbiometrics.ibscanmatcher.IBMatcherException;
import com.integratedbiometrics.ibscanmatcher.IBMatcher.Template;

/**
 * Class that provides access to a database of fingerprints.
 */
public class IBMatcherDatabase 
{
	/* *********************************************************************************************
	 * DATABASE SETUP CONSTANTS
	 ******************************************************************************************** */

	/**
	 * The name of the database.
	 */
	public static final String DATABASE_NAME            = "fingerprints.db";

	/**
	 * The database version.  If any fields are added, increment this version number and provide 
	 * logic to upgrade the database.
	 */
	public static final int    DATABASE_VERSION         = 4;

	/**
	 * The name of the table in the database.
	 */
	public static final String TABLE_NAME               = "fingerprints";

	/**
	 * The column with the ID.  This is required to use the cursor in adapters, etc., and is the 
	 * primary key.
	 */
	public static final String COLUMN_NAME__ID          = "_id";

	/**
	 * The column with the user name.
	 */
	public static final String COLUMN_NAME_NAME         = "name";

	/**
	 * The column with the description.
	 */
	public static final String COLUMN_NAME_DESCRIPTION  = "description";

	/**
	 * The column with the date this entry was created.
	 */
	public static final String COLUMN_NAME_CREATE_DATE  = "create_date";

	/**
	 * The column with the date this entry was last modified.
	 */
	public static final String COLUMN_NAME_MODIFY_DATE  = "modify_date";

	/**
	 * The column with the template.
	 */
	public static final String COLUMN_NAME_TEMPLATE     = "template";

	/**
	 * The index of the ID in the cursor returned by <code>getCursor()</code>.
	 */
	public static final int    CURSOR_INDEX__ID         = 0;

	/**
	 * The index of the name in the cursor returned by <code>getCursor()</code>.
	 */
	public static final int    CURSOR_INDEX_NAME        = 1;

	/**
	 * The index of the description in the cursor returned by <code>getCursor()</code>.
	 */
	public static final int    CURSOR_INDEX_DESCRIPTION = 2;

	/**
	 * The index of the create date in the cursor returned by <code>getCursor()</code>.
	 */
	public static final int    CURSOR_INDEX_CREATE_DATE = 3;

	/**
	 * The index of the modify date in the cursor returned by <code>getCursor()</code>.
	 */
	public static final int    CURSOR_INDEX_MODIFY_DATE = 4;
	 

	/* *********************************************************************************************
	 * PUBLIC INNER CLASSES
	 ******************************************************************************************** */

	/**
	 * Class encapsulating information associated with one entry in the database.
	 */
	public static class Entry
	{
		/*
		 * Name of the person associated with this entry.
		 */
		private final String   m_name;
		
		/*
		 * Description associated with this entry.
		 */
		private final String   m_description;
		
		/*
		 * Date on which this entry was created.
		 */
		private final Date     m_createDate;
		
		/*
		 * Date on which this entry was last modified.
		 */
		private final Date     m_modifyDate;
		
		/*
		 * Fingerprint template in this entry.
		 */
		private final Template m_template;
		
		/*
		 * Match score, if entry was returned from a match.
		 */
		private final int      m_matchScore;
	
		/*
		 * Constructor for fingerprint entries.
		 */
		protected Entry(String name, String description, Date createDate, Date modifyDate, Template template)
		{  
			this(name, description, createDate, modifyDate, template, 0);
		}
		protected Entry(String name, String description, Date createDate, Date modifyDate, 
				Template template, int matchScore)
		{  
			this.m_name        = name;
			this.m_description = description;
			this.m_createDate  = createDate;
			this.m_modifyDate  = modifyDate;
			this.m_template    = template;
			this.m_matchScore  = matchScore;
		}
	
		/**
		 * Get name of person associated with this entry.
		 * 
		 * @return name of person associated with this entry
		 */
		public String getName()
		{
			return (this.m_name);
		}
		
		/**
		 * Get description associated with this entry.
		 * 
		 * @return name of person associated with this entry
		 */
		public String getDescription()
		{
			return (this.m_description);
		}
		 
		/**
		 * Get date on which this entry was created.
		 * 
		 * @return date on which this entry was created
	     */
		public Date getCreateDate()
		{
	        return (this.m_createDate);
		}
		  
		/**
		 * Get date on which this entry was last modified.
		 * 
		 * @return date on which this entry was last modified
	     */
		public Date getModifyDate()
		{
	        return (this.m_modifyDate);
		}
		  
	    /**
         * Get fingerprint template in this entry.
		 * 
		 * @return fingerprint template in this entry
		 */
		public Template getTemplate()
		{
	        return (this.m_template);
        }
		  
	    /**
         * Get match score, if this entry was returned from a match.
		 * 
		 * @return match score
		 */
		public int getMatchScore()
		{
	        return (this.m_matchScore);
        }
	}

	/* *********************************************************************************************
	 * PUBLIC INTERFACE
	 ******************************************************************************************** */

	/**
	 * Create new database.
	 * 
	 * @param context  context for database.
	 */
	public IBMatcherDatabase(final Context context)
	{
		this.m_openHelper = new DatabaseHelper(context);
		this.m_ibMatcher  = IBMatcher.getInstance();
	}
	
	/**
	 * Get cursor for database.  The constants <code>COLUMN_INDEX_xxxx</code> correspond to the 
	 * columns of the data in the returned cursor.
	 * 
	 * @return  cursor to view database entries.
	 */
	public Cursor getCursor()
	{
		/* Open the database object in "read" mode. */
	    final SQLiteDatabase db = this.m_openHelper.getReadableDatabase();

        try
        {
        	/* 
        	 * Query database for all columns but the template.  The constants COLUMN_INDEX_#### 
        	 * correspond to the columns of the data in the returned cursor.
        	 */
 	        final Cursor cursor = db.query(
 	        		TABLE_NAME, 
 	        		new String[] {COLUMN_NAME__ID, COLUMN_NAME_NAME, COLUMN_NAME_DESCRIPTION, 
 	        				COLUMN_NAME_CREATE_DATE, COLUMN_NAME_MODIFY_DATE}, 
 	        		null, 
 	        		null, 
 	        		null, 
 	        		null, 
 	        		null);
 	        		
 	        if (cursor == null)
 	        {
 	        	Log.e(FINGERPRINT_DB_TAG, "Find failed");
 				return (null); 	        
 	        }
 	        else
 	        {
 	        	Log.i(FINGERPRINT_DB_TAG, "Found " + cursor.getCount() + " entries");
 	        	return (cursor);
 	        }
        }
        catch (final SQLException sqle)
        {
	    	Log.e(FINGERPRINT_DB_TAG, "Query failed with exception " + sqle.toString());
	    	return (null); 		        	        	        
        }
	}
	
	/**
	 * Get size of database.
	 * 
	 * @return  size of database file, in bytes.
	 */
	public long getSize()
	{
		/* Open the database object in "read" mode. */
	    final SQLiteDatabase db = this.m_openHelper.getReadableDatabase();

	    /* Get length of database file. */
        final String dbPath       = db.getPath();        
        final File   dbFile       = new File(dbPath);
        final long   dbFileLength = dbFile.length();
        
        return (dbFileLength);
	}
	
	/**
	 * Enroll user in database.
	 * 
	 * @param name        name of person to associate with this entry
	 * @param description description to associate with this entry
	 * @param template    fingerprint template
	 * @return            <code>true</code> if enrollment succeeds; <code>false</code> otherwise
	 */
	public boolean enroll(final String name, final String description, final Template template)
	{
		/* Check arguments. */
		if (name == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null name");
			throw new IllegalArgumentException("Received null name");
		}
		/* TODO: CHECK THE NAME FOR INAPPROPRIATE FORMAT */
		if (description == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null description");
			throw new IllegalArgumentException("Received null description");
		}
		/* TODO: CHECK THE DESCRIPTION FOR INAPPROPRIATE FORMAT */
		if (template == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null template");
			throw new IllegalArgumentException("Received null template");
		}
	
		/* Create the map to hold the new record's values. */
        final ContentValues values = new ContentValues();

        /* Add the date. */
        final Long now = Long.valueOf(System.currentTimeMillis());
        values.put(COLUMN_NAME_CREATE_DATE, now);
        values.put(COLUMN_NAME_MODIFY_DATE, now);

        /* Add the name. */
        values.put(COLUMN_NAME_NAME, name);
        
        /* Add the description. */
        values.put(COLUMN_NAME_DESCRIPTION, description);

        /* Add the template. */
        final byte[] templateBytes = convertTemplateToBytes(template);
        if (templateBytes == null)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to convert template to bytes");
        	return (false);
        }
        values.put(COLUMN_NAME_TEMPLATE, templateBytes);

        /* Open the database object in "write" mode. */
        final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

        try
        {
	        /* Perform the insert and returns the ID of the new note. */
	        final long rowId = db.insert(
	            TABLE_NAME,        // The table to insert into.
	            COLUMN_NAME_NAME,  // A hack, SQLite sets this column value to null if values is empty.
	            values             // A map of column names, and the values to insert into the columns.
	        );
	
	        /* If the insert succeeded, the row ID exists. */
	        if (rowId > 0)
	        {
	        	Log.i(FINGERPRINT_DB_TAG, "Create new row with row ID for user \"" + name + "\"");
	        	return (true);
	        }
	        else
	        {
	        	Log.e(FINGERPRINT_DB_TAG, "Create new row with row ID for user \"" + name + "\"");	        
	        	return (false);
	        }
        }
        catch (SQLException sqle)
        {
	    	Log.e(FINGERPRINT_DB_TAG, "Insert failed with exception " + sqle.toString());
	    	return (false); 		        	        	        
        }
	}

	/**
	 * Match template against database.
	 * 
	 * @param template  template to match
	 * @return          data base entry, if match was found; <code>null</code> otherwise
	 */
	public Entry match(final Template template)
	{
		/* Check arguments. */
		if (template == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null template");
			throw new IllegalArgumentException("Received null template");
		}

		/* Open the database object in "read" mode. */
	    final SQLiteDatabase db = this.m_openHelper.getReadableDatabase();

        try
        {
        	/* 
        	 * Get cursor with all columns.  Only the template is necessary for the match, but the
        	 * other columns will be returned in the entry.
        	 */
        	final Cursor cursor = db.query(
 	        		TABLE_NAME, 
 	        		new String[] {COLUMN_NAME_NAME, COLUMN_NAME_DESCRIPTION, 
 	        				COLUMN_NAME_CREATE_DATE, COLUMN_NAME_MODIFY_DATE, COLUMN_NAME_TEMPLATE}, 
 	        		null, 
 	        		null, 
 	        		null, 
 	        		null, 
 	        		null);
 	        		
 	        if (cursor == null)
 	        {
 	        	Log.e(FINGERPRINT_DB_TAG, "Find failed");
 				return (null); 	        
 	        }
 	        else
 	        {
 	        	Log.i(FINGERPRINT_DB_TAG, "Found " + cursor.getCount() + " entries");
 	        	cursor.moveToFirst();
 	        	
 	        	/*
 	        	 * Examine each entry, returning the first that matches.  Errors in getting data 
 	        	 * from particular entries will be logged and ignored, besides terminating 
 	        	 * examination of that particular entry.
 	        	 */
 	        	while (!cursor.isAfterLast())
 	        	{
 	 	        	/* Get name from cursor. */
 	        		final String name = cursor.getString(0);
 	 	        	if (name == null)
 	 	        	{
 	 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null name in entry");
 	 	        	}
 	 	        	else
 	 	        	{	
 	 	 	        	/* Get description from cursor. */
 	 	        		final String description = cursor.getString(1);
 	 	 	        	if (description == null)
 	 	 	        	{
 	 	 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null description in entry");
 	 	 	        	}
 	 	 	        	else
 	 	 	        	{	
		 	 	        	/* Get create date from cursor. */
 	 	 	        		final Long createDate = cursor.getLong(2);
		 	 	        	if (createDate == null)
		 	 	        	{
		 	 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null create date in entry");
		 	 	        	}
		 	 	        	else
		 	 	        	{	
			 	 	        	/* Get modify date from cursor. */
		 	 	        		final Long modifyDate = cursor.getLong(3);
			 	 	        	if (modifyDate == null)
			 	 	        	{
			 	 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null modify date in entry");
			 	 	        	}
			 	 	        	else
			 	 	        	{	
			 	 	        		/* Get template bytes from cursor. */
			 	 	        		final byte[] templateBytes = cursor.getBlob(4);
				 	 	        	if (templateBytes == null)
				 	 	        	{
				 	 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null template in entry");
				 	 	        	}
				 	 	        	else
				 	 	        	{ 	 	        	
					 	 	        	/* Convert template bytes to template. */
				 	 	        		final Template templateCompare = convertBytesToTemplate(templateBytes);
					 	 	        	if (template == null)
					 	 		        {
					 	 		        	Log.e(FINGERPRINT_DB_TAG, "Failed to convert template");
					 	 		        }
					 	 	        	else
					 	 	        	{
					 	 	        		try
					 	 	        		{
					 	 	        			/* 
					 	 	        			 * Check whether this matches our template better than the configured level. 
					 	 	        			 */
					 	 	        			final int matchScore = this.m_ibMatcher.matchTemplates(template, templateCompare);			 	 	        			
					 	 	        			if (matchScore > 0)
					 	 	        			{
					 	 	        				cursor.close();
					 	 	        			
					 	 	        				final Entry entry = new Entry(name, description, new Date(createDate), new Date(modifyDate), template, matchScore);
						 	 	 	 		        return (entry); 
					 	 	        			}
			
					 	 	        		}
					 	 	        		catch (final IBMatcherException ibme)
					 	 	        		{
					 	 	        			Log.e(FINGERPRINT_DB_TAG, "Match failed with exception " + ibme.getType().toString());
					 	 	        		}
					 	 	        	}
				 	 	        	}
			 	 	        	}
		 	 	        	}
	 	 	        	}
 	 	        	}
 	 		        cursor.moveToNext();
 	        	}
 	        	
 	        	cursor.close();
 	        	return (null);
 	        }
        }
        catch (final SQLException sqle)
        {
	    	Log.e(FINGERPRINT_DB_TAG, "Query failed with exception " + sqle.toString());
	    	return (null); 		        	        	
        }
	}
	
	/**
	 * Find entry associate with person in database.
	 * 
	 * @param name  name of person whose entry should be found
	 * @return      the first entry returned associated with the name, if successful; 
	 *              <code>null</code> otherwise
	 */
	public Entry find(final String name)
	{
		/* Check arguments. */
		if (name == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null name");
			throw new IllegalArgumentException("Received null name");
		}
		/* TODO: CHECK THE NAME FOR INAPPROPRIATE FORMAT */
		
		/* Open the database object in "read" mode. */
		final SQLiteDatabase db = this.m_openHelper.getReadableDatabase();

        try
        {
        	/* 
        	 * Query database for entries that match name.  If the database has been well-maintained,
        	 * only one entry should match.  Only the first match is returned in case of multiple
        	 * matches.
        	 */
        	final Cursor cursor = db.query(
 	        		TABLE_NAME, 
 	        		new String[] {COLUMN_NAME_DESCRIPTION, COLUMN_NAME_CREATE_DATE, COLUMN_NAME_MODIFY_DATE, COLUMN_NAME_TEMPLATE}, 
 	        		COLUMN_NAME_NAME + " = \"" + name + "\"", 
 	        		null, 
 	        		null, 
 	        		null, 
 	        		null);
 	        		
 	        if (cursor == null)
 	        {
 	        	Log.e(FINGERPRINT_DB_TAG, "Find failed for user \"" + name + "\"");
 				return (null); 	        
 	        }
 	        else
 	        {
 	        	/* 
 	        	 * Get data from first entry.  Any error in getting information about the entry 
 	        	 * will terminate processing and return a null template as if no match were found.
 	        	 */
 	        	Log.i(FINGERPRINT_DB_TAG, "Found " + cursor.getCount() + " entries for user \"" + name + "\"");
 	        	if (cursor.getCount() < 1)
 	        	{
 	        		cursor.close();
 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found no entry for user \"" + name + "\"");
 	 	        	return (null);
 	        	}
 	        	
 	        	cursor.moveToFirst();
 	        	
 	        	/* Get description from cursor. */
 	        	final String description = cursor.getString(0);
 	        	if (description == null)
 	        	{
 	        		cursor.close();
 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null description in entry for user \"" + name + "\"");
 	 	        	return (null);
 	        	}

 	        	/* Get create date from cursor. */
 	        	final Long createDate = cursor.getLong(1);
 	        	if (createDate == null)
 	        	{
 	        		cursor.close();
 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null create date in entry for user \"" + name + "\"");
 	 	        	return (null);
 	        	}

 	        	/* Get modify date from cursor. */
 	        	final Long modifyDate = cursor.getLong(2);
 	        	if (modifyDate == null)
 	        	{
 	        		cursor.close();
 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null modify date in entry for user \"" + name + "\"");
 	 	        	return (null);
 	        	}

 	        	/* Get template bytes from cursor. */
 	        	final byte[] templateBytes = cursor.getBlob(3);
 	        	if (templateBytes == null)
 	        	{
 	        		cursor.close();
 	 	        	Log.e(FINGERPRINT_DB_TAG, "Found null template in entry for user \"" + name + "\"");
 	 	        	return (null);
 	        	}
 	        	
 	        	/* Convert template bytes to template. */
 	        	final Template template = convertBytesToTemplate(templateBytes);
 	        	if (template == null)
 		        {
 	        		cursor.close();
 		        	Log.e(FINGERPRINT_DB_TAG, "Failed to convert template for user \"" + name + "\"");
 		        	return (null); 		         		        
 		        }
 		        
 	        	final Entry entry = new Entry(name, description, new Date(createDate), new Date(modifyDate), template);
 		        return (entry); 
 	        }
        }
        catch (final SQLException sqle)
        {
	    	Log.e(FINGERPRINT_DB_TAG, "Query failed for \"" + name + "\" with exception " + sqle.toString());
	    	return (null); 		        	        	
        }
	}
	
	/**
	 * Update entry associate with person in database with new template.
	 * 
	 * @param name      name of person whose entry should be updated
	 * @param template  new template for entry
	 * @return          <code>true</code> if update succeeds; <code>false</code> otherwise
	 */
	public boolean update(final String name, final Template template)
	{
		/* Check arguments. */
		if (name == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null name");
			throw new IllegalArgumentException("Received null name");
		}
		/* TODO: CHECK THE NAME FOR INAPPROPRIATE FORMAT */
		if (template == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null template");
			throw new IllegalArgumentException("Received null template");
		}

		/* Create the map to hold the new record's values. */
        final ContentValues values = new ContentValues();

        /* Add the date. */
        final Long now = Long.valueOf(System.currentTimeMillis());
        values.put(COLUMN_NAME_MODIFY_DATE, now);

        /* Add the template. */
        final byte[] templateBytes = convertTemplateToBytes(template);
        if (templateBytes == null)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to convert template to bytes");
        	return (false);
        }
        values.put(COLUMN_NAME_TEMPLATE, templateBytes);

        /* Open the database object in "write" mode. */
        final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

        try
        {
	        /* Perform the insert and returns the ID of the new note. */
	        final long count = db.update(
	            TABLE_NAME,        // The table to update.
	            values,            // A map of column names, and the values to update in the columns.
	            COLUMN_NAME_NAME + " = \"" + name + "\"",
	            null
	        );
	        
	        /* If the update succeeded, the count exists. */
	        if (count >= 0)
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Updated " + count + " rows for user \"" + name + "\"");
	       	 	return (true);
	        }
	        else
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Update failed for user \"" + name + "\"");	        
	       	 	return (false);
	        }
	    }
	    catch (SQLException sqle)
	    {
	    	Log.e(FINGERPRINT_DB_TAG, "Update failed with exception " + sqle.toString());
	    	return (false); 		        	        	        
	    }
	}
	
	/**
	 * Update entry associate with person in database with new template.
	 * 
	 * @param name         name of person whose entry should be updated
	 * @param description  new description for entry
	 * @param template     new template for entry
	 * @return             <code>true</code> if update succeeds; <code>false</code> otherwise
	 */
	public boolean update(final String name, final String description, final Template template)
	{
		/* Check arguments. */
		if (name == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null name");
			throw new IllegalArgumentException("Received null name");
		}
		/* TODO: CHECK THE NAME FOR INAPPROPRIATE FORMAT */
		if (template == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null template");
			throw new IllegalArgumentException("Received null template");
		}
		if (description == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null description");
			throw new IllegalArgumentException("Received null description");
		}
		/* TODO: CHECK THE DESCRIPTION FOR INAPPROPRIATE FORMAT */

		/* Create the map to hold the new record's values. */
        final ContentValues values = new ContentValues();

        /* Add the date. */
        final Long now = Long.valueOf(System.currentTimeMillis());
        values.put(COLUMN_NAME_MODIFY_DATE, now);

        /* Add the description. */
        values.put(COLUMN_NAME_DESCRIPTION, description);

        /* Add the template. */
        final byte[] templateBytes = convertTemplateToBytes(template);
        if (templateBytes == null)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to convert template to bytes");
        	return (false);
        }
        values.put(COLUMN_NAME_TEMPLATE, templateBytes);

        /* Open the database object in "write" mode. */
        final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

        try
        {
	        /* Perform the insert and returns the ID of the new note. */
	        final long count = db.update(
	            TABLE_NAME,        // The table to update.
	            values,            // A map of column names, and the values to update in the columns.
	            COLUMN_NAME_NAME + " = \"" + name + "\"",
	            null
	        );
	        
	        /* If the update succeeded, the count exists. */
	        if (count >= 0)
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Updated " + count + " rows for user \"" + name + "\"");
	       	 	return (true);
	        }
	        else
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Update failed for user \"" + name + "\"");	        
	       	 	return (false);
	        }
	    }
	    catch (SQLException sqle)
	    {
	    	Log.e(FINGERPRINT_DB_TAG, "Update failed with exception " + sqle.toString());
	    	return (false); 		        	        	        
	    }
	}
	
	/**
	 * Delete user's entry (or entries) from database.
	 * 
	 * @param name  name of person whose entry (or entries) should be deleted
	 * @return      <code>true</code> if one or more entries were found and deleted without
	 *              error; <code>false</code> otherwise
	 */
	public boolean delete(final String name)
	{
		/* Check arguments. */
		if (name == null)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Received null name");
			throw new IllegalArgumentException("Received null name");
		}
		/* TODO: CHECK THE NAME FOR INAPPROPRIATE FORMAT */
	
		/* Open the database object in "write" mode. */
		final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

		try
		{
	        /* Performs the delete. */
			final int count = db.delete(
	            TABLE_NAME,                      // The database table name.
	            COLUMN_NAME_NAME + " = " + name, // The final WHERE clause
	            null                             // The incoming where clause values.
	        );
	
	        /* If the delete succeeded, the count exists. */
	        if (count >= 0)
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Deleted " + count + " rows for user \"" + name + "\"");
	       	 	return (true);
	        }
	        else
	        {
	       	 	Log.i(FINGERPRINT_DB_TAG, "Delete failed for user \"" + name + "\"");	        
	       	 	return (false);
	        }
	    }
	    catch (SQLException sqle)
	    {
	    	Log.e(FINGERPRINT_DB_TAG, "Delete failed with exception " + sqle.toString());
	    	return (false); 		        	        	        
	    }
	}

	/**
	 * Clear database.
	 * 
	 * @return  <code>true</code> if one or more entries were found and deleted without
	 *          error; <code>false</code> otherwise
	 */
	public boolean clear()
	{
		/* Open the database object in "write" mode. */
		final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

		try
		{
	        /* Performs the delete. */
			final int count = db.delete(
	            TABLE_NAME,  // The database table name.
	            "1",        // The final WHERE clause
	            null         // The incoming where clause values.
	        );
	
	        /* If the delete succeeded, the count exists. */
	        if (count >= 0)
	        {
	        	Log.i(FINGERPRINT_DB_TAG, "Deleted " + count + " rows");
	        	return (true);
	        }
	        else
	        {
	        	Log.i(FINGERPRINT_DB_TAG, "Delete failed");	        
	        	return (false);
	        }
	    }
	    catch (SQLException sqle)
	    {
	    	Log.e(FINGERPRINT_DB_TAG, "Delete failed with exception " + sqle.toString());
	    	return (false); 		        	        	        
	    }
	}

	/**
	 * Reset database.
	 */
	public void reset()
	{
		/* Open the database object in "write" mode. */
		final SQLiteDatabase db = this.m_openHelper.getWritableDatabase();

       	/* Kills the table and existing data. */
       	db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

       	/* Recreates the database. */
       	this.m_openHelper.onCreate(db);
	}
	
	/* *********************************************************************************************
	 * PRIVATE INTERFACE
	 ******************************************************************************************** */

	/* 
	 * Log tag for this class.
	 */
	private static final String FINGERPRINT_DB_TAG   = "IBMatcher Database";
	
	/*
	 * The helper for accessing this database.
	 */
	private final DatabaseHelper m_openHelper;
	 
	 /*
	  * The matcher to use with this database.
	  */
	private final IBMatcher m_ibMatcher;
	
	/*
     * This class helps open, create, and upgrade the database file.
     */
  	private class DatabaseHelper extends SQLiteOpenHelper 
	{
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /*
         * Creates the underlying database,
         */
        @Override
        public void onCreate(final SQLiteDatabase db) 
        {
			try
			{
				db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
					+ COLUMN_NAME__ID + " INTEGER PRIMARY KEY,"
					+ COLUMN_NAME_NAME + " TEXT,"
					+ COLUMN_NAME_DESCRIPTION + " TEXT,"
					+ COLUMN_NAME_CREATE_DATE + " TEXT,"
					+ COLUMN_NAME_MODIFY_DATE + " TEXT,"
					+ COLUMN_NAME_TEMPLATE + " BLOB"
					+ ");");
			} 
			catch (final SQLException sqle)
			{
			   	Log.e(FINGERPRINT_DB_TAG, "Failed to create database with exception: " + sqle.toString());
			}
      	}

        /*
         * Demonstrates that the provider must consider what happens when the underlying 
         * datastore is changed. In this sample, the database is upgraded the database by 
         * destroying the existing data.  A real application should upgrade the database in place.
         */
       	@Override
       	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) 
       	{
           	Log.w(FINGERPRINT_DB_TAG, "Upgrading database from version " + oldVersion + " to "
           			+ newVersion + ", which will destroy all old data");

           	/* Kills the table and existing data. */
           	db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

           	/* Recreates the database with a new version. */
           	onCreate(db);
       	}
	}
	
	/*
	 * Convert a template to a byte array.
	 */
	private byte[] convertTemplateToBytes(final Template template)
	{
		/* Create temporary file for template. */
		final File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + "dbtemp.ibsm_template");	        	
		file.deleteOnExit();
        try
        {
        	file.createNewFile();
        }
        catch (final IOException ioe)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to create temporary file for template");
        	return (null);
        }
        	
        /* Save template to file. */
        try
        {
        	this.m_ibMatcher.saveTemplate(template, file.getAbsolutePath());	        	
        }
        catch (IBMatcherException ibme)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to save template file");
        	return (null);
        }
        
        /* Read template into byte array. */
		try
		{
			final int bytesToRead = (int)file.length();
	        if (bytesToRead <= 0)
	        {
	        	Log.e(FINGERPRINT_DB_TAG, "Failed to get length of template file");
	        	return (null);
	        }
				
	        final byte[]          templateBytes = new byte[bytesToRead];
	        final FileInputStream istream       = new FileInputStream(file);
	        final int             bytesRead     = istream.read(templateBytes);
			istream.close();				
			if (bytesRead != bytesToRead)
			{
				Log.e(FINGERPRINT_DB_TAG, "Reading template file, expected" + bytesToRead 
						+ "bytes, read only " + bytesRead + " bytes");
				return (null);
			}
			
			return (templateBytes);
		}
		catch (final IOException ioe)
		{
        	Log.e(FINGERPRINT_DB_TAG, "Failed to read template file");
        	return (null);				
		}	        	
	}
	
	/* 
	 * Convert a byte array to a template.
	 */
	private Template convertBytesToTemplate(final byte[] templateBytes)
	{
		/* Create temporary file for template. */
		final File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + "dbtemp.ibsm_template");
		file.deleteOnExit();
	    try
	    {
	    	file.createNewFile();
	    }
        catch (final IOException ioe)
        {
        	Log.e(FINGERPRINT_DB_TAG, "Failed to create temporary file for template");
        	return (null);
        }
        
        /* Write template bytes to temporary file. */
        try
        {
        	final FileOutputStream ostream = new FileOutputStream(file);
			ostream.write(templateBytes);
			ostream.close();
	    }
	    catch (IOException ioe)
	    {
	        Log.e(FINGERPRINT_DB_TAG, "Failed to write temporary file for template");
	        return (null); 		        
	    } 		        
	        
	    /* Load template from temporary file. */
	    try
	    {
	        final Template template = this.m_ibMatcher.loadTemplate(file.getAbsolutePath());
		    return (template);
	    }
	    catch (IBMatcherException ibme)
	    {
	        Log.e(FINGERPRINT_DB_TAG, "Failed to load template");
	        return (null); 		         		        
	    }
	}
}
