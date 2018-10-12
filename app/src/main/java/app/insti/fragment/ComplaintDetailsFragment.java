package app.insti.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.insti.R;
import app.insti.Utils;
import app.insti.activity.MainActivity;
import app.insti.adapter.CommentsAdapter;
import app.insti.adapter.ImageViewPagerAdapter;
import app.insti.adapter.UpVotesAdapter;
import app.insti.api.RetrofitInterface;
import app.insti.api.model.User;
import app.insti.api.model.Venter;
import app.insti.api.request.CommentCreateRequest;
import app.insti.utils.DateTimeUtil;
import de.hdodenhof.circleimageview.CircleImageView;

import me.relex.circleindicator.CircleIndicator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComplaintDetailsFragment extends Fragment {

    private final String TAG = ComplaintDetailsFragment.class.getSimpleName();
    private Venter.Complaint detailedComplaint;
    private MapView mMapView;
    private TextView textViewUserName;
    private TextView textViewReportDate;
    private TextView textViewLocation;
    private TextView textViewDescription;
    private TextView textViewCommentLabel;
    private TextView textViewVoteUpLabel;
    private TextView textViewStatus;
    private LinearLayout tagsLayout;
    private EditText editTextComment;
    private ImageButton imageButtonSend;
    private CircleImageView circleImageViewCommentUserImage;
    private RecyclerView recyclerViewComments;
    private RecyclerView recyclerViewUpVotes;
    private Button buttonVoteUp;
    private View mView;

    private static String sId, cId, uId, uProfileUrl;
    private CommentsAdapter commentListAdapter;
    private UpVotesAdapter upVotesAdapter;
    private List<Venter.Comment> commentList;
    private List<User> upVotesList;
    private LinearLayout linearLayoutTags;
    private ScrollView layoutUpVotes;
    private NestedScrollView nestedScrollView;
    private CircleIndicator circleIndicator;

    public static ComplaintDetailsFragment getInstance(String sessionid, String complaintid, String userid, String userProfileUrl) {
        sId = sessionid;
        cId = complaintid;
        uId = userid;
        uProfileUrl = userProfileUrl;
        return new ComplaintDetailsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_complaint_details, container, false);
        commentList = new ArrayList<>();

        initialiseViews(view);
        upVotesList = new ArrayList<>();
        commentListAdapter = new CommentsAdapter(getContext(), sId, uId, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        upVotesAdapter = new UpVotesAdapter(this, getContext());
        recyclerViewComments.setLayoutManager(linearLayoutManager);
        recyclerViewUpVotes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewComments.setHasFixedSize(true);
        recyclerViewUpVotes.setHasFixedSize(true);
        recyclerViewComments.setAdapter(commentListAdapter);
        recyclerViewUpVotes.setAdapter(upVotesAdapter);
        upVotesAdapter.setUpVoteList(upVotesList);
        upVotesAdapter.notifyDataSetChanged();

        mMapView = view.findViewById(R.id.google_map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        imageButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(editTextComment.getText().toString().trim().isEmpty())) {
                    postComment();
                } else {
                    Toast.makeText(getContext(), "Please enter comment text", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonVoteUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upVote(detailedComplaint);
            }
        });

        mView = view;

        return view;
    }

    private void initialiseViews(View view) {
        nestedScrollView = view.findViewById(R.id.nestedScrollViewComplaintDetail);
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewReportDate = view.findViewById(R.id.textViewReportDate);
        textViewLocation = view.findViewById(R.id.textViewLocation);
        textViewDescription = view.findViewById(R.id.textViewDescription);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewCommentLabel = view.findViewById(R.id.comment_label);
        textViewVoteUpLabel = view.findViewById(R.id.up_vote_label);
        tagsLayout = view.findViewById(R.id.tags_layout);
        linearLayoutTags = view.findViewById(R.id.linearLayoutTags);
        layoutUpVotes = view.findViewById(R.id.layoutUpVotes);
        recyclerViewComments = view.findViewById(R.id.recyclerViewComments);
        recyclerViewUpVotes = view.findViewById(R.id.recyclerViewUpVotes);
        editTextComment = view.findViewById(R.id.edit_comment);
        imageButtonSend = view.findViewById(R.id.send_comment);
        circleImageViewCommentUserImage = view.findViewById(R.id.comment_user_image);
        buttonVoteUp = view.findViewById(R.id.buttonVoteUp);
        circleIndicator  = view.findViewById(R.id.indicator);
        LinearLayout imageViewHolder = view.findViewById(R.id.image_holder_view);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT,
                        getResources().getDisplayMetrics().heightPixels / 2);
        imageViewHolder.setLayoutParams(layoutParams);
    }

    public void setDetailedComplaint(Venter.Complaint detailedComplaint) {
        this.detailedComplaint = detailedComplaint;
        populateViews();
    }

    private void populateViews() {
        try {
            buttonVoteUp.setText("UpVote");
            textViewUserName.setText(detailedComplaint.getComplaintCreatedBy().getUserName());
            String time = DateTimeUtil.getDate(detailedComplaint.getComplaintReportDate().toString());
            Log.i(TAG, " time: " + time);
            textViewReportDate.setText(time);
            textViewLocation.setText(detailedComplaint.getLocationDescription());
            textViewDescription.setText(detailedComplaint.getDescription());
            textViewStatus.setText(detailedComplaint.getStatus().toUpperCase());
            if (detailedComplaint.getStatus().equalsIgnoreCase("Reported")) {
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorRed)));
                textViewStatus.setTextColor(getContext().getResources().getColor(R.color.primaryTextColor));
            } else if (detailedComplaint.getStatus().equalsIgnoreCase("In Progress")) {
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorSecondary)));
                textViewStatus.setTextColor(getContext().getResources().getColor(R.color.secondaryTextColor));
            } else if (detailedComplaint.getStatus().equalsIgnoreCase("Resolved")) {
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorGreen)));
                textViewStatus.setTextColor(getContext().getResources().getColor(R.color.secondaryTextColor));
            }
            addTagsToView(detailedComplaint);
            if (detailedComplaint.getTags().isEmpty())
                linearLayoutTags.setVisibility(View.GONE);
            textViewCommentLabel.setText("Comments (" + detailedComplaint.getComment().size() + ")");
            textViewVoteUpLabel.setText("Up Votes (" + detailedComplaint.getUsersUpVoted().size() + ")");
            Picasso.get().load(uProfileUrl).placeholder(R.drawable.user_placeholder).into(circleImageViewCommentUserImage);
            addVotesToView(detailedComplaint);
            addCommentsToView(detailedComplaint);

            initViewPagerForImages(detailedComplaint);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mGoogleMap) {
                GoogleMap googleMap = mGoogleMap;

                // For dropping a marker at a point on the Map
                LatLng loc = new LatLng(detailedComplaint.getLatitude(), detailedComplaint.getLongitude());
                if (loc != null) {
                    googleMap.addMarker(new MarkerOptions().position(loc).title(detailedComplaint.getLatitude().toString() + " , " + detailedComplaint.getLongitude().toString()).snippet(detailedComplaint.getLocationDescription()));
                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(loc).zoom(16).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        });
    }

    private void postComment() {
        final CommentCreateRequest commentCreateRequest = new CommentCreateRequest(editTextComment.getText().toString());
        RetrofitInterface retrofitInterface = Utils.getRetrofitInterface();
        retrofitInterface.postComment("sessionid=" + sId, cId, commentCreateRequest).enqueue(new Callback<Venter.Comment>() {
            @Override
            public void onResponse(Call<Venter.Comment> call, Response<Venter.Comment> response) {
                if (response.isSuccessful()) {
                    Venter.Comment comment = response.body();
                    addNewComment(comment);
                    editTextComment.setText(null);
                }
            }

            @Override
            public void onFailure(Call<Venter.Comment> call, Throwable t) {
                Log.i(TAG, "failure in posting comments: " + t.toString());
            }
        });
    }

    private void addNewComment(Venter.Comment newComment) {
        commentList.add(newComment);
        commentListAdapter.setCommentList(commentList, textViewCommentLabel);
        commentListAdapter.notifyItemInserted(commentList.indexOf(newComment));
        commentListAdapter.notifyItemRangeChanged(0, commentListAdapter.getItemCount());
        textViewCommentLabel.setText("Comments (" + commentList.size() + ")");
        recyclerViewComments.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.hideKeyboard(getActivity());
            }
        });
    }

    private void addCommentsToView(Venter.Complaint detailedComplaint) {
        for (Venter.Comment comment : detailedComplaint.getComment())
            commentList.add(comment);
        commentListAdapter.setCommentList(commentList, textViewCommentLabel);
        commentListAdapter.notifyDataSetChanged();
    }

    private void upVote(final Venter.Complaint detailedComplaint) {
        RetrofitInterface retrofitInterface = Utils.getRetrofitInterface();
        if (detailedComplaint.getVoteCount() == 0) {
            retrofitInterface.upVote("sessionid=" + sId, cId, 1).enqueue(new Callback<Venter.Complaint>() {
                @Override
                public void onResponse(Call<Venter.Complaint> call, Response<Venter.Complaint> response) {
                    if (response.isSuccessful()) {
                        Venter.Complaint complaint = response.body();
                        detailedComplaint.setVoteCount(1);
                        addVotesToView(complaint);
                        onUpvote();
                    }
                }

                @Override
                public void onFailure(Call<Venter.Complaint> call, Throwable t) {
                    Log.i(TAG, "failure in up vote: " + t.toString());
               }
            });
        } else if (detailedComplaint.getVoteCount() ==1){
            retrofitInterface.upVote("sessionid=" + sId, cId, 0).enqueue(new Callback<Venter.Complaint>() {
                @Override
                public void onResponse(Call<Venter.Complaint> call, Response<Venter.Complaint> response) {
                    if (response.isSuccessful()) {
                        Venter.Complaint complaint = response.body();
                        detailedComplaint.setVoteCount(0);
                        addVotesToView(complaint);
                    }
                }

                @Override
                public void onFailure(Call<Venter.Complaint> call, Throwable t) {
                    Log.i(TAG, "failure in up vote: " + t.toString());
                }
            });
        }
    }

    public void addVotesToView(Venter.Complaint detailedComplaint) {
        upVotesList.clear();
        for (User users : detailedComplaint.getUsersUpVoted()) {
            upVotesList.add(users);
        }
        upVotesAdapter.setUpVoteList(upVotesList);
        upVotesAdapter.notifyDataSetChanged();
        textViewVoteUpLabel.setText("Up Votes (" + detailedComplaint.getUsersUpVoted().size() + ")");
    }

    private void onUpvote(){
        layoutUpVotes.post(new Runnable() {
            @Override
            public void run() {
                nestedScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void addTagsToView(Venter.Complaint detailedComplaint) {

        for (Venter.TagUri tagUri : detailedComplaint.getTags()) {

            TextView textViewTags = new TextView(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(10,10,10,10);
            textViewTags.setLayoutParams(layoutParams);
            textViewTags.setText(tagUri.getTagUri());
            textViewTags.setBackgroundResource(R.drawable.customborder);
            textViewTags.setPadding(30,25,30,25);
            int fontDp = 4;
            float density = getContext().getResources().getDisplayMetrics().density;
            int fontPixel = (int) (fontDp * density);
            textViewTags.setTextSize(fontPixel);
            textViewTags.setBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(R.color.colorTagGreen)));
            textViewTags.setTextColor(getContext().getResources().getColor(R.color.primaryTextColor));
            tagsLayout.setLayoutParams(layoutParams);
            tagsLayout.addView(textViewTags);
        }
    }

    private void initViewPagerForImages(Venter.Complaint detailedComplaint) {

        ViewPager viewPager = mView.findViewById(R.id.complaint_image_view_pager);
        if (viewPager != null) {
            try {
                ImageViewPagerAdapter imageFragmentPagerAdapter = new ImageViewPagerAdapter(getChildFragmentManager(), detailedComplaint);
                viewPager.setAdapter(imageFragmentPagerAdapter);
                circleIndicator.setViewPager(viewPager);
                imageFragmentPagerAdapter.registerDataSetObserver(circleIndicator.getDataSetObserver());
                Objects.requireNonNull(viewPager.getAdapter()).notifyDataSetChanged();
                synchronized (viewPager) {
                    viewPager.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}