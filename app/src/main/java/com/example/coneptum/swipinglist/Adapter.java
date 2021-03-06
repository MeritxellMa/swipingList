package com.example.coneptum.swipinglist;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Adapter
        extends ArrayAdapter<String> {

    //variables
    private int layoutResource;
    private LayoutInflater inflater;


    public Adapter(Context con, int resource, ArrayList<String> items) {
        super(con, resource, items);
        layoutResource = resource;
        inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View workingView;

        //set content layout
        if (convertView == null) {
            workingView = inflater.inflate(layoutResource, null);
        } else {
            workingView = convertView;
        }

        //get views
        final PlanetHolder holder = getPlanetHolder(workingView);
        final String entry = getItem(position);
        ListView listView = (ListView) parent;

        //set init components
        holder.name.setText(entry);

        //set visibility of deleteLayout
        holder.deleteLayout.setVisibility(View.GONE);

        //Set init parameters for mainLayout
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mainLayout.getLayoutParams();
        params.rightMargin = 0;
        params.leftMargin = 0;
        holder.mainLayout.setLayoutParams(params);

        //set SwipeDetector(class below)
        workingView.setOnTouchListener(new SwipeDetector(holder, listView));

        //set delete listener
        holder.deleteButton.setTag(position);
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.deleteLayout.isShown()) {
                    remove(getItem((int) v.getTag()));
                    notifyDataSetChanged();
                }
            }
        });

        //set onClickListener for the item
        workingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "hadles onClickListenerEvent", Toast.LENGTH_SHORT).show();
                //TODO set click listener for the view here, not in OnItemClickListener
            }
        });


        return workingView;
    }


    private PlanetHolder getPlanetHolder(View workingView) {
        Object tag = workingView.getTag();
        PlanetHolder holder;

        if (tag == null || !(tag instanceof PlanetHolder)) {

            holder = new PlanetHolder();

            //layout to swipe
            holder.mainLayout = (LinearLayout) workingView.findViewById(R.id.mainview);
            //layout displayed when swiping
            holder.deleteLayout = (RelativeLayout) workingView.findViewById(R.id.deleteview);
            //button to click when background layout is displayed
            holder.deleteButton = (ImageView) workingView.findViewById(R.id.delete_button);

            //other views
            holder.name = (TextView) workingView.findViewById(R.id.planet_name);
            /* other views here */


            workingView.setTag(holder);
        } else {
            holder = (PlanetHolder) tag;
        }

        return holder;
    }


    public static class PlanetHolder {
        public LinearLayout mainLayout;
        public RelativeLayout deleteLayout;
        public ImageView deleteButton;

        public TextView name;
        /* other views here */
    }


    public class SwipeDetector implements View.OnTouchListener {

        //constants
        private final int MAX_LOCK_DISTANCE; // max swiped distance
        private final int MIN_LOCK_DISTANCE; //min swiped distance
        private static final int MAX_DP_SWIPE=100;

        //variables
        private boolean motionInterceptDisallowed = false; //lock other listeners
        private float downX, upX, downY, upY; //initial and final points
        private int rightMargin, leftMargin; // right and left margin of mainLayout
        private PlanetHolder holder;
        private ListView listView;


        public SwipeDetector(PlanetHolder h, ListView listView) {
            holder = h;
            this.listView = listView;
            MAX_LOCK_DISTANCE = (int) (MAX_DP_SWIPE*(getContext().getResources().getDisplayMetrics().density));
            MIN_LOCK_DISTANCE = MAX_LOCK_DISTANCE / 10;
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {


                //first touch
                case MotionEvent.ACTION_DOWN: {

                    //initial point
                    downX = event.getX();
                    Log.i("downX", downX + "");
                    downY = event.getY();
                    Log.i("downY", downY + "");

                    //set rightMargin in case of needing (used on swipeRight method)
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mainLayout.getLayoutParams();
                    rightMargin = params.rightMargin;
                    leftMargin = params.leftMargin;

                    return true; // allow other events like Click to be processed
                }


                //when drag (each different point detected from initial point)
                case MotionEvent.ACTION_MOVE: {

                    //detected point
                    upX = event.getX();
                    Log.i("upX", upX + "");
                    upY = event.getY();
                    Log.i("upY", upY + "");

                    //distance between initial and detected point
                    float deltaX = downX - upX;
                    Log.i("deltaX", deltaX + "");

                    //lock other listeners
                    if (deltaX > MIN_LOCK_DISTANCE && listView != null && !motionInterceptDisallowed) {
                        listView.requestDisallowInterceptTouchEvent(true);
                        motionInterceptDisallowed = true;
                    }

                    //hide other items and show selected item
                    if (listView != null) {
                        for (int i = 0; i < listView.getCount(); i++) {
                            if (i != listView.getSelectedItemPosition()) {
                                updateView(i);
                            }
                        }
                    }
                    holder.deleteLayout.setVisibility(View.VISIBLE);

                    //swipe
                    if (deltaX > MAX_LOCK_DISTANCE) {
                        //lock when MAX_LOCK_DISTANCE is reached
                        swipeLeft(-MAX_LOCK_DISTANCE, holder.mainLayout);
                    } else if (deltaX < -MAX_LOCK_DISTANCE) {
                        //lock when normal position is reached
                        swipeLeft(0, holder.mainLayout);
                    } else if (rightMargin == 0 && deltaX < 0) {
                        //don't swipe right if background layout is not shown
                        swipeLeft(0, holder.mainLayout);
                    } else if (deltaX > 0) {
                        swipeLeft(-(int) deltaX, holder.mainLayout);
                    } else {
                        swipeRight((int) deltaX);
                    }

                    return true;
                }


                //final touch
                case MotionEvent.ACTION_UP:

                    //distance
                    float deltaX = downX - upX;
                    Log.i("up deltaX", deltaX + "");

                    float deltaY = downY - upY;
                    Log.i("up deltaY", deltaY + "");

                    //set background state: open or closed
                    setStateOfBackgroundLayout((int) deltaX, deltaY, v);


                    //active other listeners
                    if (listView != null) {
                        listView.requestDisallowInterceptTouchEvent(false);
                        motionInterceptDisallowed = false;
                    }

                    return true;


                //on gesture interrupted
                //does the same as ACTION_UP
                case MotionEvent.ACTION_CANCEL:

                    //distance
                    deltaX = downX - upX;
                    Log.i("cancel delta", deltaX + "");
                    deltaY = downY - upY;
                    Log.i("cancel deltaY", deltaY + "");

                    //set background state: open or closed
                    setStateOfBackgroundLayout((int) deltaX, deltaY, v);

                    //active other listeners
                    if (listView != null) {
                        listView.requestDisallowInterceptTouchEvent(false);
                        motionInterceptDisallowed = false;
                    }

                    return false;
            }

            return true;
        }

        /**
         * Set background layout state: open or closed
         *
         * @param distanceX swipe horizontal distance
         * @param distanceY swipe vertical distance
         * @param v         view to swipe
         */
        private void setStateOfBackgroundLayout(int distanceX, float distanceY, View v) {
            if ((distanceX == 0 && distanceY == 0 || distanceX == downX && distanceY == downY) && rightMargin == 0) {
                //single click, call onClickListener of selected view
                holder.deleteLayout.setVisibility(View.GONE);
                v.performClick();
            } else if (distanceX < MIN_LOCK_DISTANCE) {
                //do not show background layout when MIN_LOCK_DISTANCE is not reached
                swipeLeft(0, holder.mainLayout);
                //disallow deleteButton
                holder.deleteLayout.setVisibility(View.GONE);
            } else if (holder.deleteLayout.isShown()) {
                //deleteLayout is shown when trying to drag it out
                swipeLeft(-MAX_LOCK_DISTANCE, holder.mainLayout);
            }
            upY = 0;
            upX = 0;
        }

        /**
         * Deletes right margin
         *
         * @param distance swiping distance
         */
        private void swipeRight(int distance) {
            View animationView = holder.mainLayout;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) animationView.getLayoutParams();
            params.rightMargin = rightMargin + distance;
            params.leftMargin = leftMargin - distance;
            animationView.setLayoutParams(params);
        }

        /**
         * Add right margin and deletes left margin of animationView
         *
         * @param distance      swiping distance
         * @param animationView view to swipe
         */
        private void swipeLeft(int distance, View animationView) {
            //View animationView = holder.mainLayout;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) animationView.getLayoutParams();
            params.rightMargin = -distance;
            params.leftMargin = distance;
            animationView.setLayoutParams(params);
        }

        /**
         * hide background view if it is shown
         *
         * @param index of the view to update
         */
        private void updateView(int index) {
            View v = listView.getChildAt(index -
                    listView.getFirstVisiblePosition());

            if (v == null)
                return;

            RelativeLayout deleteLayout = (RelativeLayout) v.findViewById(R.id.deleteview);
            if (deleteLayout.isShown()) {
                LinearLayout mainLayout = (LinearLayout) v.findViewById(R.id.mainview);
                swipeLeft(0, mainLayout);
                deleteLayout.setVisibility(View.GONE);
            }
        }
    }
}
