package com.ak.uobtimetable.Utilities;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.ak.uobtimetable.API.Service;
import com.ak.uobtimetable.API.Models;

/**
 * Wrapper around SharedPreferences, with one method per setting in this application.
 */
public class SettingsManager {

    private Context context;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    public static final int SETTINGS_VERSION = 1;
    public static final String PREF_NAME = "prefs";
    public static final int PREF_INDEX = 0;

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static SettingsManager instance;

    private enum Settings {
        refreshWiFi,
        refreshCellular,
        savedSessions,
        savedSessionsTime,
        savedCourse,
        longRoomNames,
        showHiddenSessions,
        launchCount,
        shownRateDialog,
        completedTutorial
    }

    public SettingsManager(Context context) {

        this.context = context;
        this.sharedPrefs = context.getSharedPreferences(PREF_NAME, PREF_INDEX);
        this.editor = sharedPrefs.edit();
    }

    public static SettingsManager getInstance(Context context){

        if (instance == null)
            instance = new SettingsManager(context);

        return instance;
    }

    private String keyPrefix(){

        return "v" + SETTINGS_VERSION + "_";
    }

    private String keyToString(Settings key){

        return  keyPrefix() + key.name();
    }

    public boolean isEmpty(){

        return sharedPrefs.getAll().size() == 0;
    }

    public boolean clearOldData(){

        boolean removed = false;
        Set<String> keys = sharedPrefs.getAll().keySet();
        for (String key : keys) {
            if (key.startsWith(keyPrefix()) == false) {
                Logger.getInstance().info("SettingsManager", "Deleted old key: " + key);
                editor.remove(key);
                removed = true;
            }
        }

        return removed;
    }

    public String[] getAllKeys(){

        return sharedPrefs.getAll().keySet().toArray(new String[0]);
    }

    private String getString(Settings key, String defaultValue) {

        Logger.getInstance().info("SettingsManager", "Getting string: " + key);
        return sharedPrefs.getString(keyToString(key), defaultValue);
    }

    private void setString(Settings key, String value) {

        Logger.getInstance().info("SettingsManager", "Setting string: " + key);
        editor.putString(keyToString(key), value).commit();
        new BackupManager(context).dataChanged();
    }

    private boolean getBool(Settings key, boolean defValue) {

        Logger.getInstance().info("SettingsManager", "Getting boolean: " + key);
        return sharedPrefs.getBoolean(keyToString(key), defValue);
    }

    private void setBool(Settings key, boolean value) {

        Logger.getInstance().info("SettingsManager", "Setting boolean: " + key);
        editor.putBoolean(keyToString(key), value).commit();
        new BackupManager(context).dataChanged();
    }

    private int getInt(Settings key, int defaultValue){

        Logger.getInstance().info("SettingsManager", "Getting int: " + key);
        return sharedPrefs.getInt(keyToString(key), defaultValue);
    }

    private void setInt(Settings key, int value){

        Logger.getInstance().info("SettingsManager", "Setting int: " + key);
        editor.putInt(keyToString(key), value).commit();
        new BackupManager(context).dataChanged();
    }

    private Date getDate(Settings key, Date defValue){

        Logger.getInstance().info("SettingsManager", "Getting date: " + key);
        String dateString = sharedPrefs.getString(keyToString(key), null);

        if (dateString == null)
            return defValue;

        try {
            return DATE_FORMAT.parse(dateString);
        } catch (Exception e) {
            Logger.getInstance().error("SettingsManager", "Saved date has incorrect format: " + dateString);
            return defValue;
        }
    }

    private void setDate(Settings key, Date value){

        Logger.getInstance().info("SettingsManager", "Setting date: " + key);
        editor.putString(keyToString(key), DATE_FORMAT.format(value)).commit();
        new BackupManager(context).dataChanged();
    }

    public int incrementLaunchCount(){

        Settings key = Settings.launchCount;
        int newValue = getInt(key, 0) + 1;
        setInt(key, newValue);

        return newValue;
    }

    public int getLaunchCount(){
        return getInt(Settings.launchCount, 0);
    }

    // Custom
    public boolean getLongRoomNames(){
        return getBool(Settings.longRoomNames, false);
    }

    public void setLongRoomNames(boolean longRoomNames){
        setBool(Settings.longRoomNames, longRoomNames);
    }

    public boolean getRefreshWiFi(){
        return getBool(Settings.refreshWiFi, true);
    }

    public void setRefreshWiFi(boolean value){
        setBool(Settings.refreshWiFi, value);
    }

    public boolean getRefreshCellular(){
        return getBool(Settings.refreshCellular, true);
    }

    public void setRefreshCellular(boolean value){
        setBool(Settings.refreshCellular, value);
    }

    public boolean getShowHiddenSessions(){

        return getBool(Settings.showHiddenSessions, false);
    }

    public void setShowHiddenSessions(boolean hidden){

        setBool(Settings.showHiddenSessions, hidden);
    }

    public boolean toggleShowHiddenSessions(){

        boolean showHiddenSessions = !getShowHiddenSessions();
        setShowHiddenSessions(showHiddenSessions);
        return showHiddenSessions;
    }

    public boolean hasCourse(){

        return getCourse() != null;
    }

    public Models.Course getCourse(){

        String courseJson = getString(Settings.savedCourse, null);

        if (courseJson == null)
            return null;

        return Service.makeGson().fromJson(courseJson, Models.Course.class);
    }

    public void setCourse(Models.Course course){
        String courseJson = Service.makeGson().toJson(course);
        setString(Settings.savedCourse, courseJson);
    }

    public List<Models.DisplaySession> getSessions(){

        String sessionsJson = getString(Settings.savedSessions, null);

        if (sessionsJson == null)
            return null;

        Type sessionListType = new TypeToken<ArrayList<Models.DisplaySession>>(){}.getType();
        return Service.makeGson().fromJson(sessionsJson, sessionListType);
    }

    public boolean hasSessions(){

        return getSessions() != null;
    }

    public void setSessions(List<Models.DisplaySession> sessions, boolean updateTime){

        Type sessionListType = new TypeToken<ArrayList<Models.DisplaySession>>(){}.getType();
        String sessionsJson = Service.makeGson().toJson(sessions, sessionListType);
        setString(Settings.savedSessions, sessionsJson);

        if (updateTime)
            setDate(Settings.savedSessionsTime, new Date());
    }

    public String getSessionsUpdatedTimeAgo(){

        Date sessionsUpdatedDate = getDate(Settings.savedSessionsTime, null);

        if (sessionsUpdatedDate == null)
            return null;

        return GeneralUtilities.getDateTimeAgo(sessionsUpdatedDate);
    }

    public void setShownRateDialog(){

        setBool(Settings.shownRateDialog, true);
    }

    public boolean getShownRateDialog(){

        return getBool(Settings.shownRateDialog, false);
    }

    public void setCompletedTutorial(){

        setBool(Settings.completedTutorial, true);
    }

    public boolean getCompletedTutorial(){

        return getBool(Settings.completedTutorial, false);
    }

    public void clear(){

        Logger.getInstance().debug("SettingsManager", "Cleared");
        editor.clear().commit();
    }
}