package ca.mahram.android.runtimepermissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 A simple {@link Fragment} subclass. The ugliest in existence. Not even banks write UI this ugly.
 I challenge you to write a fragment uglier than this beast.
 */
public class DangerousFragment
  extends Fragment
  implements LoaderManager.LoaderCallbacks<Cursor>, LocationListener {

    private static final long  MIN_LOCATION_UPDATE_TIME = 5000;
    private static final float MIN_DISTANCE_DELTA       = 400f;

    private static final int REQUEST_PERMISSIONS = 101;

    @Bind (R.id.last_call_number) TextView lastCallNumber;
    @Bind (R.id.latitude)         TextView latitude;
    @Bind (R.id.longitude)        TextView longitude;

    private static final int LOADER_LAST_CALL = 0;

    private AlertDialog dialog;

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
    }

    @SuppressLint ("InlinedApi") // I guess I should have picked a better permission. Meh.
    @Override public void onStart () {
        super.onStart ();

        final Context context = getContext ();

        final Set<String> requiredPermissions = new HashSet<> ();

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission (context,
                                                                                    Manifest.permission.READ_CALL_LOG))
            requiredPermissions.add (Manifest.permission.READ_CALL_LOG);

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission (context,
                                                                                    Manifest.permission
                                                                                      .ACCESS_FINE_LOCATION))
            requiredPermissions.add (Manifest.permission.ACCESS_FINE_LOCATION);

        // has both permissions. We're good to go!
        if (requiredPermissions.isEmpty ()) {
            onCallLogPermissionGranted ();
            onLocationPermissionGranted ();
            return;
        }

        // has location permission
        if (!requiredPermissions.contains (Manifest.permission.ACCESS_FINE_LOCATION))
            onLocationPermissionGranted ();

        // has call log permission
        if (!requiredPermissions.contains (Manifest.permission.READ_CALL_LOG))
            onCallLogPermissionGranted ();

        if (showPermissionRationaleDialogMaybe (context, requiredPermissions))
            return;

        requestPermissions (requiredPermissions);
    }

    private void requestPermissions (final Set<String> requiredPermissions) {
        requestPermissions (requiredPermissions.toArray (new String[requiredPermissions.size ()]), REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult (final int requestCode,
                                            final String[] permissions,
                                            final int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        // this isn't the request you're looking for
        if (requestCode != REQUEST_PERMISSIONS)
            return;

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                onPermissionGranted (permissions[i]);
            else
                onPermissionDenied (permissions[i]);
        }
    }

    private void onPermissionDenied (final String permission) {
        if (Manifest.permission.READ_CALL_LOG.equals (permission))
            onCallLogPermissionDenied ();
        else if (Manifest.permission.ACCESS_FINE_LOCATION.equals (permission))
            onLocationPermissionDenied ();
    }

    private void onPermissionGranted (final String permission) {
        if (Manifest.permission.READ_CALL_LOG.equals (permission))
            onCallLogPermissionGranted ();
        else if (Manifest.permission.ACCESS_FINE_LOCATION.equals (permission))
            onLocationPermissionGranted ();
    }

    private boolean showPermissionRationaleDialogMaybe (final Context context, final Set<String> requiredPermissions) {
        final boolean needCallLogRationale = shouldShowRequestPermissionRationale (Manifest.permission.READ_CALL_LOG);
        final boolean needLocationRationale =
          shouldShowRequestPermissionRationale (Manifest.permission.ACCESS_FINE_LOCATION);

        if (!needCallLogRationale && !needLocationRationale)
            // not showing a rationale dialog
            return false;

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener () {
            @Override public void onClick (final DialogInterface dialog, final int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    requestPermissions (requiredPermissions);
                    return;
                }

                if (requiredPermissions.contains (Manifest.permission.READ_CALL_LOG))
                    onCallLogPermissionDenied ();

                if (requiredPermissions.contains (Manifest.permission.ACCESS_FINE_LOCATION))
                    onLocationPermissionDenied ();
            }
        };

        final View dialogBody = View.inflate (context, R.layout.dialog_combined_rationale, null);

        final DialogBody body = new DialogBody (dialogBody);

        if (needCallLogRationale)
            body.callLogRationale.setText (R.string.call_log_rationale);
        else
            body.callLogRationale.setVisibility (View.GONE);

        if (needLocationRationale)
            body.locationRationale.setText (R.string.location_rationale);
        else
            body.locationRationale.setVisibility (View.GONE);

        dialog = new AlertDialog.Builder (context)
                   .setTitle (R.string.multi_rationale_dialog_title)
                   .setView (dialogBody)
                   .setPositiveButton (android.R.string.ok, listener)
                   .setNegativeButton (R.string.creepy, listener)
                   .create ();

        dialog.show ();
        return true;
    }

    @Override public void onPause () {
        if (null != dialog) {
            dialog.dismiss ();
            dialog = null;
        }

        if (null != gps) {
            //noinspection ResourceType
            gps.removeUpdates (this);
        }

        super.onPause ();
    }

    @SuppressWarnings ("ResourceType")
    private void onLocationPermissionGranted () {
        final Activity activity = getActivity ();
        gps = (LocationManager) activity.getSystemService (Context.LOCATION_SERVICE);
        updateLocation (gps.getLastKnownLocation (LocationManager.NETWORK_PROVIDER));
        gps.requestLocationUpdates (LocationManager.GPS_PROVIDER, MIN_LOCATION_UPDATE_TIME, MIN_DISTANCE_DELTA, this);
    }

    private void onLocationPermissionDenied () {
        gps = null;
        latitude.setText (R.string.denied);
        longitude.setText (R.string.denied);
    }

    private void onCallLogPermissionGranted () {
        lastCallNumber.setText (R.string.loading);
        final LoaderManager lm = getLoaderManager ();
        lm.initLoader (LOADER_LAST_CALL, null, this);
    }

    private void onCallLogPermissionDenied () {
        lastCallNumber.setText (R.string.denied);
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
            lastCallNumber.setText (R.string.call_log_empty);
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

    static final class DialogBody {
        @Bind (R.id.call_log_rationale) TextView callLogRationale;
        @Bind (R.id.location_rationale) TextView locationRationale;

        DialogBody (final View dialog) {
            ButterKnife.bind (this, dialog);
        }
    }
}