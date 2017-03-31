package mz.maputobustrackerModule.adapter;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;

import mz.maputobustrackerModule.domain.Tripulante;


public class UserRecyclerAdapter extends FirebaseRecyclerAdapter<Tripulante, UserViewHolder> {

    public UserRecyclerAdapter(
            Class<Tripulante> modelClass,
            int modelLayout,
            Class<UserViewHolder> viewHolderClass,
            Query ref ){
        super( modelClass, modelLayout, viewHolderClass, ref );
    }
    @Override
    protected void populateViewHolder(
            UserViewHolder userViewHolder,
            Tripulante tripulante, int i) {
        userViewHolder.text1.setText( tripulante.getName() );
        userViewHolder.text2.setText( tripulante.getEmail() );
    }
}