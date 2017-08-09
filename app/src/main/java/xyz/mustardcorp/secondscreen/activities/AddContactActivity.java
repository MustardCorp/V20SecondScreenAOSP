package xyz.mustardcorp.secondscreen.activities;

import android.content.ContentUris;
import android.content.pm.PackageInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.Contact;

import static xyz.mustardcorp.secondscreen.misc.Util.openDisplayPhoto;

public class AddContactActivity extends AppCompatActivity
{
    private String whichContact = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_app_shortcut);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            whichContact = extras.getString(xyz.mustardcorp.secondscreen.layouts.Contacts.CONTACT_ID);
            Log.e("CONTACT", whichContact);
        }

        LoadContacts.newInstance(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String getWhichContact() {
        return whichContact;
    }

    private static class LoadContacts extends AsyncTask<Void, Void, Void> {
        private WeakReference<AddContactActivity> mContext;
        private CustomRecyclerAdapter mAdapter;

        public static LoadContacts newInstance(AddContactActivity context) {
            LoadContacts contacts = new LoadContacts();
            contacts.mContext = new WeakReference<>(context);
            return contacts;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            mAdapter = CustomRecyclerAdapter.newInstance(mContext.get());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            mContext.get().findViewById(R.id.apps_loading).setVisibility(View.GONE);
            RecyclerView recyclerView = (RecyclerView) mContext.get().findViewById(R.id.app_list_rv);
            recyclerView.setVisibility(View.VISIBLE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(mContext.get());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                    layoutManager.getOrientation());
            recyclerView.addItemDecoration(dividerItemDecoration);
        }
    }

    public static class CustomRecyclerAdapter extends RecyclerView.Adapter<CustomRecyclerAdapter.CustomViewHolder> {

        private TreeMap<String, Contact> contacts;
        private WeakReference<AddContactActivity> mActivity;

        public static CustomRecyclerAdapter newInstance(AddContactActivity activity) {
            CustomRecyclerAdapter adapter = new CustomRecyclerAdapter();
            adapter.contacts = new TreeMap<>(activity.compileContactsList());
            adapter.mActivity = new WeakReference<>(activity);
            return adapter;
        }

        @Override
        public int getItemCount()
        {
            return contacts.size();
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.layout_contact, parent, false);
            return new CustomViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position)
        {
            holder.setContactName(((Contact)contacts.values().toArray()[position]).name);
            holder.setContactIcon(((Contact)contacts.values().toArray()[position]).avatar);
            holder.setContactNumber(((Contact)contacts.values().toArray()[position]).number);
            holder.setContactId(((Contact)contacts.values().toArray()[position]).id);
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            private View mView;
            private final TextView contactName;
            private final ImageView contactIcon;
            private String contactNumber = null;
            private long contactId = -1;

            public CustomViewHolder (View v) {
                super(v);
                mView = v;

                v.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        if (mActivity.get().getWhichContact() != null) {
                            Settings.Global.putString(mActivity.get().getContentResolver(), mActivity.get().getWhichContact(), String.valueOf(getContactId()));
                            mActivity.get().finish();
                        }
                    }
                });

                contactName = mView.findViewById(R.id.contact_name);
                contactIcon = mView.findViewById(R.id.contact_icon);
            }

            public void setContactName(String name) {
                contactName.setText(name);
            }

            public void setContactIcon(Drawable icon) {
                contactIcon.setImageDrawable(icon);
            }

            public void setContactNumber(String number) {
                contactNumber = number;
            }

            public void setContactId(long id) {contactId = id;}

            public String getContactName() {
                return contactName.getText().toString();
            }

            public Drawable getContactIcon() {
                return contactIcon.getDrawable();
            }

            public String getContactNumber() {
                return contactNumber;
            }

            public long getContactId() { return contactId; }
        }

    }

    private TreeMap<String, Contact> compileContactsList() {
        TreeMap<String, Contact> contacts = new TreeMap<>();

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, "display_name");
        while (phones.moveToNext())
        {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String id = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

            Contact contact = new Contact();
            contact.name = name;
            contact.number = phoneNumber;
            contact.id = Long.decode(id);

            InputStream inputStream = openDisplayPhoto(this, contact.id);
            contact.avatar = Drawable.createFromStream(inputStream, "avatar");

            contacts.put(contact.name, contact);
        }
        phones.close();

        return contacts;
    }
}
