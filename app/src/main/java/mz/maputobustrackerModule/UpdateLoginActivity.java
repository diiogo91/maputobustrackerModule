package mz.maputobustrackerModule;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import mz.maputobustrackerModule.domain.Tripulante;

public class UpdateLoginActivity extends AppCompatActivity implements ValueEventListener {
    private Toolbar toolbar;
    private Tripulante tripulante;
    private AutoCompleteTextView newEmail;
    private EditText password;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_login);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init(){
        toolbar.setTitle( getResources().getString(R.string.update_login) );
        newEmail = (AutoCompleteTextView) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);

        tripulante = new Tripulante();
        tripulante.setId( mAuth.getCurrentUser().getUid() );
        tripulante.contextDataDB( this );
    }

    public void update( View view ){

        tripulante.setPassword( password.getText().toString() );

        reauthenticate();
    }

    private void reauthenticate(){
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if( firebaseUser == null ){
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                tripulante.getEmail(),
                tripulante.getPassword()
        );

        firebaseUser.reauthenticate( credential )
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if( task.isSuccessful() ){
                        updateData();
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseCrash.report( e );
                    Toast.makeText(
                            UpdateLoginActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
    }

    private void updateData(){
        tripulante.setPassword( password.getText().toString() );

        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if( firebaseUser == null ){
            return;
        }

        firebaseUser
            .updateEmail( newEmail.getText().toString() )
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if( task.isSuccessful() ){
                        tripulante.setEmail( newEmail.getText().toString() );
                        tripulante.updateDB();

                        Toast.makeText(
                                UpdateLoginActivity.this,
                                "Email de login atualizado com sucesso",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            })
            .addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseCrash.report( e );
                    Toast.makeText(
                            UpdateLoginActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Tripulante u = dataSnapshot.getValue( Tripulante.class );
        newEmail.setText( u.getEmail() );
        tripulante.setEmail( u.getEmail() );
    }

    @Override
    public void onCancelled(DatabaseError firebaseError) {
        FirebaseCrash.report( firebaseError.toException() );
    }
}