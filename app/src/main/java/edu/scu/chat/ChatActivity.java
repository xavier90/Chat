package edu.scu.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.scu.chat.Utils.ChatHelper;
import edu.scu.chat.Utils.Utils;
import edu.scu.chat.View.ChatMessageBoxView;

public class ChatActivity extends AppCompatActivity {

    private DatabaseReference messageChatDatabaseRef;
    private String recipientId;
    private String senderId;
    private FirebaseRecyclerAdapter<Message, MessageViewHolder> mFirebaseAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private static final String TAG = "ChatActivity";
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private EditText mMessageEditText;
    private ChatMessageBoxView messageBoxView;
    private RecyclerView previousMessages;
    private MessageListAdapter messageListAdapter;
    private ChildEventListener messageChatListener;
    private ChatHelper chatHelper;
    private FirebaseUser mFirebaseUser;
    private String mPhotoUrl;

    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";


    //define message view class
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recipientId = getIntent().getStringExtra(Utils.CONTACT_ID);
        senderId = getIntent().getStringExtra(Utils.CURRENT_USER_ID);
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mFirebaseUser.getPhotoUrl() != null) {
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }





//        final SwipeRefreshLayout mSwipeRefresh = (SwipeRefreshLayout)findViewById(R.id.ptr_layout);
//        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                if (DEBUG) Timber.d("onRefreshStarted");
//
//                BNetworkManager.sharedManager().getNetworkAdapter().loadMoreMessagesForThread(thread)
//                        .done(new DoneCallback<List<BMessage>>() {
//                            @Override
//                            public void onDone(List<BMessage> bMessages) {
//                                if (DEBUG)
//                                    Timber.d("New messages are loaded, Amount: %s", (bMessages == null ? "No messages" : bMessages.size()));
//
//                                if (bMessages.size() < 2)
//                                    showToast(getString(R.string.chat_activity_no_more_messages_to_load_toast));
//                                else {
//                                    // Saving the position in the list so we could back to it after the update.
//                                    chatSDKChatHelper.loadMessages(true, false, -1, messagesListAdapter.getCount() + bMessages.size());
//                                }
//
//                                mSwipeRefresh.setRefreshing(false);
//                            }
//                        })
//                        .fail(new FailCallback<Void>() {
//                            @Override
//                            public void onFail(Void aVoid) {
//                                mSwipeRefresh.setRefreshing(false);
//                            }
//                        });
//            }
//        });

        //load previous message
        messageListAdapter = new MessageListAdapter(new ArrayList<Message>(), getApplicationContext());
        previousMessages = (RecyclerView) findViewById(R.id.list_chat);
        previousMessages.setLayoutManager(new LinearLayoutManager(this));
        previousMessages.setHasFixedSize(true);
        previousMessages.setAdapter(messageListAdapter);
        messageChatDatabaseRef = FirebaseDatabase.getInstance().getReference("messages");


        messageChatListener = messageChatDatabaseRef.limitToFirst(20).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildKey) {

                if(dataSnapshot.exists()){
                    Message newMessage = dataSnapshot.getValue(Message.class);
                    if(newMessage.getsenderId().equals(senderId)){
                        if (newMessage.getImageUrl() == null) {
                            newMessage.setRecipientOrSenderStatus(MessageListAdapter.SENDER_TEXT);
                        } else {
                            newMessage.setRecipientOrSenderStatus(MessageListAdapter.SENDER_IMAGE);
                        }

                    }else{
                        if (newMessage.getImageUrl() == null) {
                            newMessage.setRecipientOrSenderStatus(MessageListAdapter.RECIPIENT_TEXT);
                        } else {
                            newMessage.setRecipientOrSenderStatus(MessageListAdapter.RECIPIENT_IMAGE);
                        }
                    }
                    messageListAdapter.refillAdapter(newMessage);
                    previousMessages.scrollToPosition(messageListAdapter.getItemCount()-1);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //handle how to send message
        messageBoxView = (ChatMessageBoxView) findViewById(R.id.message_box);
        chatHelper = new ChatHelper(messageBoxView, messageChatDatabaseRef, mFirebaseUser, senderId, recipientId);
        chatHelper.setMessageListAdapter(messageListAdapter);
        chatHelper.setActivity(this);
        messageBoxView.setMessageSendListener(chatHelper);
        messageBoxView.setMessageBoxOptionsListener(chatHelper);

//        messageChatDatabaseRef = FirebaseDatabase.getInstance().getReference("messages").addChildEventListener(new ChildEventListener() {
//                });

//        previousMessage = (RecyclerView) findViewById(R.id.messageRecyclerView);
//        mLinearLayoutManager = new LinearLayoutManager(this);
//        mLinearLayoutManager.setStackFromEnd(true);
//
//        mFirebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(
//        Message.class,
//        R.layout.text_message_recipient,
//        MessageViewHolder.class,
//        messageChatDatabaseRef.child("messages")){
//
//            @Override
//            protected Message parseSnapshot(DataSnapshot snapshot) {
//                Message friendlyMessage = super.parseSnapshot(snapshot);
//                friendlyMessage.setContactId(contactID);
//                friendlyMessage.setCurrentUserId(mCurrentUserId);
//                if (friendlyMessage != null && friendlyMessage.isShow(snapshot.getKey())) {
//
//                    friendlyMessage.setId(snapshot.getKey());
//                }
//                return friendlyMessage;
//            }
//
//            @Override
//            protected void populateViewHolder(final MessageViewHolder viewHolder,
//                                              Message friendlyMessage, int position) {
//                if (friendlyMessage.getText() != null) {
//                    viewHolder.messageTextView.setText(friendlyMessage.getText());
//                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
//                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
//                } else {
//                    String imageUrl = friendlyMessage.getImageUrl();
//                    if (imageUrl.startsWith("gs://")) {
//                        StorageReference storageReference = FirebaseStorage.getInstance()
//                                .getReferenceFromUrl(imageUrl);
//                        storageReference.getDownloadUrl().addOnCompleteListener(
//                                new OnCompleteListener<Uri>() {
//                                    @Override
//                                    public void onComplete(@NonNull Task<Uri> task) {
//                                        if (task.isSuccessful()) {
//                                            String downloadUrl = task.getResult().toString();
//                                            Glide.with(viewHolder.messageImageView.getContext())
//                                                    .load(downloadUrl)
//                                                    .into(viewHolder.messageImageView);
//                                        } else {
//                                            Log.w(TAG, "Getting download url was not successful.",
//                                                    task.getException());
//                                        }
//                                    }
//                                });
//                    } else {
//                        Glide.with(viewHolder.messageImageView.getContext())
//                                .load(friendlyMessage.getImageUrl())
//                                .into(viewHolder.messageImageView);
//                    }
//                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
//                    viewHolder.messageTextView.setVisibility(TextView.GONE);
//                }
//
//
//                viewHolder.messengerTextView.setText(friendlyMessage.getName());
//                if (friendlyMessage.getPhotoUrl() == null) {
//                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(ChatActivity.this,
//                            R.drawable.ic_account_circle_black_36dp));
//                } else {
//                    Glide.with(ChatActivity.this)
//                            .load(friendlyMessage.getPhotoUrl())
//                            .into(viewHolder.messengerImageView);
//                }
//
//                if (friendlyMessage.getText() != null) {
//                    // write this message to the on-device index
//                    FirebaseAppIndex.getInstance().update(getMessageIndexable(friendlyMessage));
//                }
//
//                // log a view action on it
//                FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage));
//            }
//        };
//
//
//
//        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
//            @Override
//            public void onItemRangeInserted(int positionStart, int itemCount) {
//                super.onItemRangeInserted(positionStart, itemCount);
//                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
//                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
//                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
//                // to the bottom of the list to show the newly added message.
//                if (lastVisiblePosition == -1 ||
//                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
//                    previousMessage.scrollToPosition(positionStart);
//                }
//            }
//        });
//
//        previousMessage.setLayoutManager(mLinearLayoutManager);
//        previousMessage.setAdapter(mFirebaseAdapter);
//
//
//        // Initialize Firebase Measurement.
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
//
//        // Initialize Firebase Remote Config.
//        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
//
//        // Define Firebase Remote Config Settings.
//        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
//                new FirebaseRemoteConfigSettings.Builder()
//                        .setDeveloperModeEnabled(true)
//                        .build();
//
//        // Define default config values. Defaults are used when fetched config values are not
//        // available. Eg: if an error occurred fetching values from the server.
//        Map<String, Object> defaultConfigMap = new HashMap<>();
//        defaultConfigMap.put("friendly_msg_length", 50L);
//
//        // Apply config settings and default values.
//        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
//        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
//
//        // Fetch remote config.
//        fetchConfig();
//
//        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
//                .getInt(FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
//        mMessageEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (charSequence.toString().trim().length() > 0) {
//                    mSendButton.setEnabled(true);
//                } else {
//                    mSendButton.setEnabled(false);
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//            }
//        });
//
//        mAddMessageImageView = (ImageView) findViewById(R.id.btn_options);
//        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("image/*");
//                startActivityForResult(intent, REQUEST_IMAGE);
//            }
//        });
//
//        mSendButton = (Button) findViewById(R.id.btn_chat_send_message);
//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Message friendlyMessage = new Message(mMessageEditText.getText().toString(), mUsername,
//                        mPhotoUrl, null);
//                messageChatDatabaseRef.child(MESSAGES_CHILD).push().setValue(friendlyMessage);
//                mMessageEditText.setText("");
//                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
//            }
//        });
}

//    private Action getMessageViewAction(Message friendlyMessage) {
//        return new Action.Builder(Action.Builder.VIEW_ACTION)
//                .setObject(friendlyMessage.getName(), MESSAGE_URL.concat(friendlyMessage.getId()))
//                .setMetadata(new Action.Metadata.Builder().setUpload(false))
//                .build();
//    }
//
//    private Indexable getMessageIndexable(Message friendlyMessage) {
//        PersonBuilder sender = Indexables.personBuilder()
//                .setIsSelf(mUsername.equals(friendlyMessage.getName()))
//                .setName(friendlyMessage.getName())
//                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/sender"));
//
//        PersonBuilder recipient = Indexables.personBuilder()
//                .setName(mUsername)
//                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/recipient"));
//
//        Indexable messageToIndex = Indexables.messageBuilder()
//                .setName(friendlyMessage.getText())
//                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId()))
//                .setSender(sender)
//                .setRecipient(recipient)
//                .build();
//
//        return messageToIndex;
//    }
//
////     Fetch the config to determine the allowed length of messages.
//    public void fetchConfig() {
//        long cacheExpiration = 3600; // 1 hour in seconds
//        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
//        // server. This should not be used in release builds.
//        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
//            cacheExpiration = 0;
//        }
//        mFirebaseRemoteConfig.fetch(cacheExpiration)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
//                        mFirebaseRemoteConfig.activateFetched();
//                        applyRetrievedLengthLimit();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // There has been an error fetching the config
//                        Log.w(TAG, "Error fetching config", e);
//                        applyRetrievedLengthLimit();
//                    }
//                });
//    }
//
//    private void applyRetrievedLengthLimit() {
//        Long friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
//        Log.d(TAG, "FML is: " + friendly_msg_length);
//    }
//
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    // TODO: 6/13/17
                    //loading image url should be image message url
                    Message tempMessage = new Message(null, mPhotoUrl,
                            LOADING_IMAGE_URL, senderId, recipientId);
                    messageChatDatabaseRef.push()
                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        String key = databaseReference.getKey();
                                        StorageReference storageReference =
                                                FirebaseStorage.getInstance()
                                                        .getReference(mFirebaseUser.getUid())
                                                        .child(key)
                                                        .child(uri.getLastPathSegment());

                                        putImageInStorage(storageReference, uri, key);

                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }

            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }


    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(ChatActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            Message friendlyMessage =
                                    new Message(null, mPhotoUrl,
                                            task.getResult().getDownloadUrl()
                                                    .toString(), senderId, recipientId);
                            messageChatDatabaseRef.child(key).setValue(friendlyMessage);

//                            messageListAdapter.refillAdapter(friendlyMessage);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(messageChatListener != null) {
            messageChatDatabaseRef.removeEventListener(messageChatListener);
        }
        messageListAdapter.cleanUp();

    }

}