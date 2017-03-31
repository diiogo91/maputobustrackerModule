package mz.maputobustrackerModule;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import mz.maputobustrackerModule.adapter.UserRecyclerAdapter;
import mz.maputobustrackerModule.domain.Tripulante;
import mz.maputobustrackerModule.domain.util.LibraryClass;


public class MainActivity extends AppCompatActivity implements ValueEventListener {

    private DatabaseReference databaseReference;
    public Tripulante u;
    private FirebaseAuth mAuth;
    private UserRecyclerAdapter adapter;
    private FirebaseAuth.AuthStateListener authStateListener;
    private Tripulante op;
    private TextView txtNomeUt;
    private Button btnStart;
    private Button btnStop;
    private Intent i;
    private Button btnStatus;
    protected ProgressBar progressBar;
    private Snackbar snackbar;

    public static List<LatLng> polyroute;
    private ProgressDialog progressDialog;
    private SharedPreferences permissionStatus;
    private boolean sentToSettings = false;
    private static final int PERMISSION_CALLBACK_CONSTANT = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    String[] permissionsRequired = new String[]{android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.GET_ACCOUNTS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if( firebaseAuth.getCurrentUser() == null  ){
                    Intent intent = new Intent( MainActivity.this, LoginActivity.class );
                    startActivity( intent );
                    finish();
                }
            }
        };
        txtNomeUt = (TextView) findViewById(R.id.txtNomeUt);
        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener( authStateListener );
        databaseReference = LibraryClass.getFirebase();
        btnStart= (Button) findViewById(R.id.btnStartService);
        btnStatus= (Button) findViewById(R.id.btnStatus);
        btnStart.setEnabled(false);
        btnStatus.setEnabled(false);
        progressBar = (ProgressBar) findViewById(R.id.progressMain);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Iniciando Módulo Maputo Bus Tracker, Por favor aguarde...");
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        progressDialog.setCancelable(false);

        progressDialog.setOnDismissListener(new ProgressDialog.OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface dialog) {
                permissionStatus = getSharedPreferences("permissionStatus",MODE_PRIVATE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[2]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[3]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[4]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[5]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[6]) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[7]) != PackageManager.PERMISSION_GRANTED
                        ) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[0])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[1])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[2])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[3])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[4])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[5])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[6])
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissionsRequired[7])) {
                        //Show Information about why you need the permission
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Aplicação necessita de Permissões");
                        builder.setMessage("Esta aplicação necessita de múltiplas permissões para o seu normal funcionamento");
                        builder.setPositiveButton("Garantir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(MainActivity.this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT);
                            }
                        });
                        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                snackbar= Snackbar.make(progressBar, "Para usufruir das funcionalidades da aplicação queira por favor atribuir permissões a mesma.", Snackbar.LENGTH_INDEFINITE);
                                View snackBarView = snackbar.getView();
                                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                snackbar.show();
                            }
                        });
                        builder.show();
                    } else if (permissionStatus.getBoolean(permissionsRequired[0], false)) {
                        //Previously Permission Request was cancelled with 'Dont Ask Again',
                        // Redirect to Settings after showing Information about why you need the permission
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Aplicação necessita de Permissões");
                        builder.setMessage("Esta aplicação necessita de múltiplas permissões para o seu normal funcionamento");
                        builder.setPositiveButton("Garantir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                sentToSettings = true;
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                                Toast.makeText(getBaseContext(), "Garanta as permissões a aplicação nas configurações do seu Smartphone", Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                Toast.makeText(getBaseContext(),"",Toast.LENGTH_LONG).show();
                                snackbar= Snackbar.make(progressBar, "Para usufruir das funcionalidades da aplicação queira por favor atribuir permissões a mesma.", Snackbar.LENGTH_INDEFINITE);
                                View snackBarView = snackbar.getView();
                                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                snackbar.show();
                            }
                        });
                        builder.show();
                    } else {
                        //just request the permission
                        ActivityCompat.requestPermissions(MainActivity.this, permissionsRequired, PERMISSION_CALLBACK_CONSTANT);
                    }

                    Toast.makeText(getBaseContext(), "Permissões Requeridas", Toast.LENGTH_LONG).show();

                    SharedPreferences.Editor editor = permissionStatus.edit();
                    editor.putBoolean(permissionsRequired[0], true);
                    editor.commit();
                } else {
                    //You already have the permission, just go ahead.
                    btnStart.setEnabled(true);
                    btnStatus.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CALLBACK_CONSTANT){
            //check if all permissions are granted
            boolean allgranted = false;
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i]==PackageManager.PERMISSION_GRANTED){
                    allgranted = true;
                } else {
                    allgranted = false;
                    break;
                }
            }

            if(allgranted){
                proceedAfterPermission();
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[1])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[2])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[3])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[4])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[5])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[6])
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,permissionsRequired[7])){
                Toast.makeText(getBaseContext(),"Permissões Requeridas",Toast.LENGTH_LONG).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Aplicação necessita de Permissões");
                builder.setMessage("Esta aplicação necessita de múltiplas permissões para o seu normal funcionamento");
                builder.setPositiveButton("Garantir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        ActivityCompat.requestPermissions(MainActivity.this,permissionsRequired,PERMISSION_CALLBACK_CONSTANT);
                    }
                });
                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Toast.makeText(getBaseContext(),"A aplicação não ira funcionar normalmente sem as Permissões Requeridas",Toast.LENGTH_LONG).show();
                    }
                });
                builder.show();
            } else {
                Toast.makeText(getBaseContext(),"Impossivel obter permissões",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                proceedAfterPermission();
            }
        }
    }

    private void proceedAfterPermission() {
        Toast.makeText(getBaseContext(), "Todas as permissões foram garantidas", Toast.LENGTH_LONG).show();
        btnStart.setEnabled(true);
        btnStatus.setEnabled(true);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (sentToSettings) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED) {
                //Got Permission
                proceedAfterPermission();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    public void startService(View view)
    {
        System.out.println(u.getName());
        i = new Intent(MainActivity.this, MapsActivity.class);
        i.putExtra("operador",u);
        startActivity(i);
    }

    public void statusService(View view)
    {
        System.out.println(u.getName());
        i = new Intent(MainActivity.this, StatusActivity.class);
        i.putExtra("operador",u);
        startActivity(i);
    }
    private void init(){
        FirebaseUser userFirebase = FirebaseAuth.getInstance().getCurrentUser();
        op = new Tripulante();
        op.setId( userFirebase.getUid());
        op.contextDataDB( this );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( authStateListener != null ){
            mAuth.removeAuthStateListener( authStateListener );
        }
    }



    // MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Tripulante utilizador = new Tripulante();

        if( utilizador.isSocialNetworkLogged( this ) ){
            getMenuInflater().inflate(R.menu.menu_social_network_logged, menu);
        }
        else{
            getMenuInflater().inflate(R.menu.menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if(id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        u = dataSnapshot.getValue( Tripulante.class );
        txtNomeUt.setText( u.getName());
        progressDialog.dismiss();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        FirebaseCrash.report( databaseError.toException() );
    }
}