package ca.mahram.android.runtimepermissions;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    static class Holder
      extends RecyclerView.ViewHolder {

        @Bind (android.R.id.title) TextView text;

        public Holder (final View itemView) {
            super (itemView);
            ButterKnife.bind (this, itemView);
        }
    }
}
