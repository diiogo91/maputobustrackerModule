package mz.maputobustrackerModule;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import java.util.Arrays;


import mz.maputobustrackerModule.domain.Tripulante;


public class LoginActivity extends CommonActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN_GOOGLE = 7859;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference databaseReference;
    private Tripulante utilizador;
    private CallbackManager callbackManager;
    private GoogleApiClient mGoogleApiClient;
    private Snackbar snackbar;

    private TwitterAuthClient twitterAuthClient;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // FACEBOOK
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                accessFacebookLoginData( loginResult.getAccessToken() );
            }

            @Override
            public void onCancel() {}

            @Override
            public void onError(FacebookException error) {
                FirebaseCrash.report( error );
                showSnackbar( error.getMessage() );
            }
        });

        // GOOGLE SIGN IN
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("484900037187-rcqsk83tgukfn7pbn4trucpe92hfe6oj.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = getFirebaseAuthResultHandler();
        initViews();
        initUser();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == RC_SIGN_IN_GOOGLE ){

            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent( data );
            GoogleSignInAccount account = googleSignInResult.getSignInAccount();

            if( account == null ){
                showSnackbar("Google login falhou, tente novamente");
                return;
            }

            accessGoogleLoginData( account.getIdToken() );
        }
        else{

            callbackManager.onActivityResult( requestCode, resultCode, data );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        verifyLogged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if( mAuthListener != null ){
            mAuth.removeAuthStateListener( mAuthListener );
        }
    }

    private void accessFacebookLoginData(AccessToken accessToken){
        accessLoginData(
                "facebook",
                (accessToken != null ? accessToken.getToken() : null)
        );
    }

    private void accessGoogleLoginData(String accessToken){
        accessLoginData(
                "google",
                accessToken
        );
    }

    private void accessLoginData( String provider, String... tokens ){
        if( tokens != null
                && tokens.length > 0
                && tokens[0] != null ){

            AuthCredential credential = FacebookAuthProvider.getCredential( tokens[0]);
            credential = provider.equalsIgnoreCase("google") ? GoogleAuthProvider.getCredential( tokens[0], null) : credential;
            utilizador.saveProviderSP( LoginActivity.this, provider );
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if( !task.isSuccessful() ){
                                showSnackbar("Login social falhou");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            FirebaseCrash.report( e );
                        }
                    });
        }
        else{
            mAuth.signOut();
        }
    }

    private FirebaseAuth.AuthStateListener getFirebaseAuthResultHandler(){
        FirebaseAuth.AuthStateListener callback = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser userFirebase = firebaseAuth.getCurrentUser();

                if( userFirebase == null ){
                    return;
                }

                if( utilizador.getId() == null
                        && isNameOk(utilizador, userFirebase ) ){

                    utilizador.setId( userFirebase.getUid() );
                    utilizador.setNameIfNull( userFirebase.getDisplayName() );
                    utilizador.setEmailIfNull( userFirebase.getEmail() );
                    utilizador.saveDB();
                }

                callMainActivity();
            }
        };
        return( callback );
    }

    private boolean isNameOk(Tripulante utilizador, FirebaseUser firebaseUser ){
        return(
                utilizador.getName() != null
                        || firebaseUser.getDisplayName() != null
        );
    }





    protected void initViews(){
        email = (AutoCompleteTextView) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.login_progress);
    }

    protected void initUser(){
        utilizador = new Tripulante();
        utilizador.setEmail( email.getText().toString() );
        utilizador.setPassword( password.getText().toString() );
    }

    public void callSignUp(View view){
        Intent intent = new Intent( this, SignUpActivity.class );
        startActivity(intent);
    }

    public void callReset(View view){
        Intent intent = new Intent( this, ResetActivity.class );
        startActivity(intent);
    }

    public void sendLoginData( View view ){
        FirebaseCrash.log("LoginActivity:clickListener:button:sendLoginData()");
        openProgressBar();
        initUser();
        verifyLogin();
    }

    public void sendLoginFacebookData( View view ){
        FirebaseCrash.log("LoginActivity:clickListener:button:sendLoginFacebookData()");
        LoginManager
                .getInstance()
                .logInWithReadPermissions(
                        this,
                        Arrays.asList("public_profile", "user_friends", "email")
                );
    }

    public void sendLoginGoogleData( View view ){

        FirebaseCrash.log("LoginActivity:clickListener:button:sendLoginGoogleData()");

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
    }




    private void callMainActivity(){
        Intent intent = new Intent( this, MainActivity.class );
        startActivity(intent);
        finish();
    }

    private void verifyLogged(){

        if( mAuth.getCurrentUser() != null ){
            callMainActivity();
        }
        else{
            mAuth.addAuthStateListener( mAuthListener );
        }
    }

    private void verifyLogin(){
        FirebaseCrash.log("LoginActivity:verifyLogin()");
        utilizador.saveProviderSP( LoginActivity.this, "" );
        if(utilizador.getEmail().equals(null)|| utilizador.getEmail().equals("") || utilizador.getPassword().equals(null) || utilizador.getPassword().equals(""))
        {
            showSnackbar("Por Favor, Introduze as Credenciais");
            progressBar.setVisibility(View.INVISIBLE);
        }
        else
        {

            mAuth.signInWithEmailAndPassword(
                    utilizador.getEmail(),
                    utilizador.getPassword()
            )
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if( !task.isSuccessful() ){
                                showSnackbar("Login falhou");
                                progressBar.setVisibility(View.INVISIBLE);
                                return;
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseCrash.report( e );
                }
            });
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        FirebaseCrash.report( new Exception( connectionResult.getErrorCode()+": "+connectionResult.getErrorMessage() ) );
        showSnackbar( connectionResult.getErrorMessage() );
    }
}