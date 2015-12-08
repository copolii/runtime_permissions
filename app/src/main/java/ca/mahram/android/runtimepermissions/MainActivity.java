package ca.mahram.android.runtimepermissions;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 Created by mahramf on 09/10/15.
 */
public class MainActivity
  extends AppCompatActivity
  implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CONTACTS_LOADER = 1;
    private static final String DANGER_FRAGMENT = "danger";

    @Bind (android.R.id.list) RecyclerView list;

    private final List<String> names = new ArrayList<> ();

    private ContactsAdapter adapter;

    @Override protected void onCreate (final Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        ButterKnife.bind (this);

        list.setLayoutManager (new LinearLayoutManager (this));

        adapter = new ContactsAdapter ();
        list.setAdapter (adapter);

        getSupportLoaderManager ().initLoader (CONTACTS_LOADER, null, this);
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
                names.add (data.getString (1));
            } while (data.moveToNext ());
        }

        adapter.notifyDataSetChanged ();
    }

    @Override public void onLoaderReset (final Loader<Cursor> loader) {
        if (CONTACTS_LOADER != loader.getId ())
            throw new IllegalArgumentException ("Unknown loader id: " + loader.getId ());

        names.clear ();
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
