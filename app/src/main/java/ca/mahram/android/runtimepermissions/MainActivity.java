package ca.mahram.android.runtimepermissions;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 Created by mahramf on 09/10/15.
 */
public class MainActivity
  extends AppCompatActivity
  implements LoaderManager.LoaderCallbacks<Cursor>, DialogInterface.OnClickListener,
             ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String DANGER_FRAGMENT = "danger";

    private static final String LOGTAG = "CONTACTIVITY";

    private static final int CONTACTS_LOADER       = 1;
    private static final int PERM_REQUEST_CONTACTS = 1;
    private static final int REQUEST_SETTINGS      = 1;

    @Bind (android.R.id.list) RecyclerView list;

    private final List<String> names = new ArrayList<> ();

    private ContactsAdapter adapter;

    private AlertDialog dialog;

    @Override protected void onCreate (final Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        ButterKnife.bind (this);

        list.setLayoutManager (new LinearLayoutManager (this));

        adapter = new ContactsAdapter ();
        list.setAdapter (adapter);
        checkContactsReadPermission ();
    }

    @Override protected void onPause () {
        if (null != dialog) {
            dialog.dismiss ();
            dialog = null;
        }

        super.onPause ();
    }

    private void checkContactsReadPermission () {
        Toast.makeText (this, R.string.checking_permission, Toast.LENGTH_SHORT).show ();
        // check for contacts permission
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission (this,
                                                                                    Manifest.permission
                                                                                      .READ_CONTACTS)) {
            Log.d (LOGTAG, "Permission already granted");
            onContactsPermissionGranted ();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale (this, Manifest.permission.READ_CONTACTS)) {
            Log.d (LOGTAG, "Permission not granted, show rationale dialog");
            showPermissionRationaleDialog ();
        } else {
            Log.d (LOGTAG, "Permission not granted, don't show rationale dialog");
            requestReadContactsPermission ();
        }
    }

    private void showPermissionRationaleDialog () {
        Log.d (LOGTAG, "showing permission rationale dialog");
        dialog = new AlertDialog.Builder (this)
                   .setTitle (R.string.contacts_access)
                   .setMessage (R.string.contacts_permission_rationale)
                   .setPositiveButton (R.string.your_contacts, this)
                   .setNegativeButton (R.string.no_way, this)
                   .setCancelable (false)
                   .create ();
        dialog.show ();
    }

    private void requestReadContactsPermission () {
        Toast.makeText (this, R.string.requesting_permission, Toast.LENGTH_SHORT).show ();
        Log.d (LOGTAG, "Requesting permission");
        ActivityCompat.requestPermissions (this,
                                           new String[] {Manifest.permission.READ_CONTACTS},
                                           PERM_REQUEST_CONTACTS);
    }

    private void onContactsPermissionGranted () {
        Toast.makeText (this, R.string.permission_granted, Toast.LENGTH_SHORT).show ();
        Log.d (LOGTAG, "Permission granted. Initializing loader.");
        getSupportLoaderManager ().initLoader (CONTACTS_LOADER, null, this);
    }

    private void onContactsPermissionDenied () {
        Toast.makeText (this, R.string.permission_denied, Toast.LENGTH_SHORT).show ();
        Log.d (LOGTAG, "Permission denied. Bailing.");

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener () {
            @Override public void onClick (final DialogInterface dialog, final int which) {
                if (which != DialogInterface.BUTTON_NEUTRAL) {
                    finish ();
                    return;
                }

                gotoSettings ();
            }
        };

        dialog = new AlertDialog.Builder (this)
                   .setTitle (R.string.no_soup)
                   .setMessage (R.string.contacts_permission_denied)
                   .setPositiveButton (android.R.string.ok, listener)
                   .setNeutralButton (R.string.change, listener)
                   .setCancelable (true)
                   .setOnCancelListener (new DialogInterface.OnCancelListener () {
                       @Override public void onCancel (final DialogInterface dialog) {
                           finish ();
                       }
                   })
                   .create ();
        dialog.show ();
    }

    private void gotoSettings () {
        final Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            intent = new Intent (Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                 Uri.fromParts ("package", getPackageName (), null));
        } else {
            intent = new Intent (Intent.ACTION_VIEW);
            intent.setClassName ("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO
                             ? "pkg"
                             : "com.android.settings.ApplicationPkgName"
                              , getPackageName ());
        }

        startActivityForResult (intent, REQUEST_SETTINGS);
    }

    @Override protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult (requestCode, resultCode, data);

        if (requestCode != REQUEST_SETTINGS)
            return;

        Log.d (LOGTAG, "Returned from settings with result " + resultCode);
        checkContactsReadPermission ();
    }

    @Override
    public void onRequestPermissionsResult (final int requestCode,
                                            @NonNull final String[] permissions,
                                            @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);

        Log.d (LOGTAG,
               String.format (Locale.ENGLISH,
                              "Request permission result for request %d.\nPermissions: %s.\nResults: %s",
                              requestCode,
                              Arrays.toString (permissions),
                              Arrays.toString (grantResults)));

        if (requestCode != PERM_REQUEST_CONTACTS)
            return;

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            onContactsPermissionGranted ();
        else
            onContactsPermissionDenied ();
    }

    @Override public Loader<Cursor> onCreateLoader (final int id, final Bundle args) {
        if (CONTACTS_LOADER != id)
            throw new IllegalArgumentException ("Unknown loader id: " + id);

        return new CursorLoader (this, ContactsContract.Contacts.CONTENT_URI,
                                 new String[] {ContactsContract.Contacts._ID,
                                               ContactsContract.Contacts.DISPLAY_NAME},
                                 null,
                                 null,
                                 ContactsContract.Contacts.DISPLAY_NAME + " ASC");
    }

    @Override public void onLoadFinished (final Loader<Cursor> loader, final Cursor data) {
        if (CONTACTS_LOADER != loader.getId ())
            throw new IllegalArgumentException ("Unknown loader id: " + loader.getId ());

        names.clear ();

        if (data.moveToFirst ()) {
            do {
                final String name = data.getString (1);

                if (TextUtils.isEmpty (name))
                    continue;

                names.add (name);
            } while (data.moveToNext ());
        }

        adapter.notifyDataSetChanged ();
    }

    @Override public void onLoaderReset (final Loader<Cursor> loader) {
        if (CONTACTS_LOADER != loader.getId ())
            throw new IllegalArgumentException ("Unknown loader id: " + loader.getId ());

        names.clear ();
    }

    @Override public void onClick (final DialogInterface dialog, final int which) {
        if (DialogInterface.BUTTON_POSITIVE == which) {
            requestReadContactsPermission ();
        } else { // denied
            onContactsPermissionDenied ();
        }
    }

    private class ContactsAdapter
      extends RecyclerView.Adapter {

        private final LayoutInflater inflater = LayoutInflater.from (MainActivity.this);

        @Override public RecyclerView.ViewHolder onCreateViewHolder (final ViewGroup parent, final int viewType) {
            final View item = inflater.inflate (R.layout.item_contact, parent, false);
            return new Holder (item);
        }

        @Override public void onBindViewHolder (final RecyclerView.ViewHolder holder, final int position) {
            ((Holder) holder).text.setText (names.get (position));
        }

        @Override public int getItemCount () {
            return names.size ();
        }
    }

    @Override public boolean onCreateOptionsMenu (final Menu menu) {
        super.onCreateOptionsMenu (menu);

        final MenuItem record = menu.add (0, R.id.action_danger, 0, R.string.dangerous_action)
                                    .setIcon (R.drawable.ic_action_lock);

        MenuItemCompat.setShowAsAction (record, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override public boolean onOptionsItemSelected (final MenuItem item) {
        if (item.getItemId () != R.id.action_danger)
            return super.onOptionsItemSelected (item);

        doSomethingDangerous ();
        return true;
    }

    private void doSomethingDangerous () {
        final FragmentManager fm = getSupportFragmentManager ();

        if (fm.findFragmentByTag (DANGER_FRAGMENT) != null)
            return;

        // show the fragment
        Toast.makeText (this, "Danger! Danger!", Toast.LENGTH_SHORT).show ();

        final FragmentTransaction ft = fm.beginTransaction ();
        ft.add (android.R.id.custom, new DangerousFragment (), DANGER_FRAGMENT);
        ft.addToBackStack (DANGER_FRAGMENT);
        ft.commit ();
    }

    static class Holder
      extends RecyclerView.ViewHolder {

        @Bind (android.R.id.title) TextView text;

        public Holder (final View itemView) {
            super (itemView);
            ButterKnife.bind (this, itemView);
        }
    }
}
