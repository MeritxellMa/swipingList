package com.example.coneptum.swipinglist;

import android.content.Context;
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
    //views
    private ListView listView;

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
        listView = (ListView) parent;

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
                } else {
                    v.performClick();
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
        private static final int MAX_LOCK_DISTANCE = 150; // max swiped distance
        private static final int MIN_LOCK_DISTANCE = MAX_LOCK_DISTANCE / 5; //min swiped distance

        //variables
        private boolean motionInterceptDisallowed = false; //lock other listeners
        private float downX, upX; //initial and final points
        private int rightMargin; // right margin of mainLayout
        private PlanetHolder holder;
        private ListView listView;


        public SwipeDetector(PlanetHolder h, ListView listView) {
            holder = h;
            this.listView = listView;
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {


                //first touch
                case MotionEvent.ACTION_DOWN: {

                    //initial point
                    downX = event.getX();
                    Log.i("downX", downX + "");

                    //set rightMargin in case of needing (used on swipeRight method)
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mainLayout.getLayoutParams();
                    rightMargin = params.rightMargin;

                    return true; // allow other events like Click to be processed
                }


                //when drag (each different point detected from initial point)
                case MotionEvent.ACTION_MOVE: {

                    //detected point
                    upX = event.getX();
                    Log.i("upX", upX + "");

                    //distance between initial and detected point
                    float deltaX = downX - upX;
                    Log.i("deltaX", deltaX + "");

                    //lock other listeners
                    if (listView != null && !motionInterceptDisallowed) {
                        listView.requestDisallowInterceptTouchEvent(true);
                        motionInterceptDisallowed = true;
                    }

                    //hide other items and show selected item
                    for (int i = 0; i < listView.getCount(); i++) {
                        if (i != listView.getSelectedItemPosition()) {
                            updateView(i);
                        }
                    }
                    holder.deleteLayout.setVisibility(View.VISIBLE);

                    //swipe
                    if (deltaX > MAX_LOCK_DISTANCE) {
                        //lock when MAX_LOCK_DISTANCE is reached
                        swipeLeft(-MAX_LOCK_DISTANCE, holder.mainLayout);
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

                    //set background state: open or closed
                    setStateOfBackgroundLayout((int) deltaX, v);

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

                    setStateOfBackgroundLayout((int) deltaX, v);

                    return false;
            }

            return true;
        }

        /**
         * Set background layout state: open or closed
         *
         * @param distance
         * @param v
         */
        private void setStateOfBackgroundLayout(int distance, View v) {
            if (distance == 0 && rightMargin == 0) {
                //single click, call onClickListener of the selected view
                holder.deleteLayout.setVisibility(View.GONE);
                v.performClick();
            } else if (distance < MIN_LOCK_DISTANCE) {
                //do not show background layout when MIN_LOCK_DISTANCE is not reached
                swipeLeft(0, holder.mainLayout);
                //disallow deleteButton
                holder.deleteLayout.setVisibility(View.GONE);
            } else if (holder.deleteLayout.isShown()) {
                //deleteLayout is shown when trying to drag it out
                swipeLeft(-MAX_LOCK_DISTANCE, holder.mainLayout);
            }

        }

        /**
         * Deletes right margin
         *
         * @param distance
         */
        private void swipeRight(int distance) {
            View animationView = holder.mainLayout;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) animationView.getLayoutParams();
            params.rightMargin = rightMargin + distance;
            animationView.setLayoutParams(params);
        }

        /**
         * Adds right margin and deletes left margin
         *
         * @param distance
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
         * @param index
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
