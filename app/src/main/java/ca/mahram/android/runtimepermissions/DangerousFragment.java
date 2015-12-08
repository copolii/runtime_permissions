package ca.mahram.android.runtimepermissions;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 A simple {@link Fragment} subclass.
 */
public class DangerousFragment
  extends Fragment
  implements LoaderManager.LoaderCallbacks<Cursor>, LocationListener {

    private static final long  MIN_LOCATION_UPDATE_TIME = 5000;
    private static final float MIN_DISTANCE_DELTA       = 400f;

    @Bind (R.id.last_call_number) TextView lastCallNumber;
    @Bind (R.id.latitude)         TextView latitude;
    @Bind (R.id.longitude)        TextView longitude;

    private static final int LOADER_LAST_CALL = 0;

    private LocationManager gps;

    public DangerousFragment () {
        // Required empty public constructor
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate (R.layout.fragment_dangerous, container, false);
    }

    @Override public void onViewCreated (final View view, final Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);
        ButterKnife.bind (this, view);

        final LoaderManager lm = getLoaderManager ();
        lm.initLoader (LOADER_LAST_CALL, null, this);
    }

    @Override public void onAttach (final Activity activity) {
        super.onAttach (activity);
        gps = (LocationManager) activity.getSystemService (Context.LOCATION_SERVICE);
    }

    @Override public void onDetach () {
        gps.removeUpdates (this);
        super.onDetach ();
    }

    @Override public void onPause () {
        gps.removeUpdates (this);
        super.onPause ();
    }

    @Override public void onResume () {
        super.onResume ();
        updateLocation (gps.getLastKnownLocation (LocationManager.NETWORK_PROVIDER));
        gps.requestLocationUpdates (LocationManager.GPS_PROVIDER, MIN_LOCATION_UPDATE_TIME, MIN_DISTANCE_DELTA, this);
    }

    private void updateLocation (final Location location) {
        if (null == location)
            return;

        latitude.setText (String.valueOf (location.getLatitude ()));
        longitude.setText (String.valueOf (location.getLongitude ()));
    }

    @Override public void onDestroyView () {
        super.onDestroyView ();
        ButterKnife.unbind (this);
    }

    @Override public Loader<Cursor> onCreateLoader (final int id, final Bundle args) {
        final Activity activity = getActivity ();

        if (null == activity || LOADER_LAST_CALL != id)
            return null;

        return new CursorLoader (activity,
                                 CallLog.Calls.CONTENT_URI,
                                 new String[] {CallLog.Calls.NUMBER},
                                 CallLog.Calls.TYPE + "=? OR " + CallLog.Calls.TYPE + "=?",
                                 new String[] {String.valueOf (CallLog.Calls.INCOMING_TYPE),
                                               String.valueOf (CallLog.Calls.MISSED_TYPE)},
                                 CallLog.Calls.DATE + " DESC");
    }

    @Override public void onLoadFinished (final Loader<Cursor> loader, final Cursor cursor) {
        if (!cursor.moveToFirst ()) {
            lastCallNumber.setText (R.string.failed);
            return;
        }

        lastCallNumber.setText (cursor.getString (0));
    }

    @Override public void onLoaderReset (final Loader<Cursor> loader) {

    }

    @Override public void onLocationChanged (final Location location) {
        updateLocation (location);
    }

    @Override public void onStatusChanged (final String provider, final int status, final Bundle extras) {
        // meh
    }

    @Override public void onProviderEnabled (final String provider) {
        // meh
    }

    @Override public void onProviderDisabled (final String provider) {
        // meh
    }
}