package mz.maputobustrackerModule;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import mz.maputobustrackerModule.domain.Associacao;
import mz.maputobustrackerModule.domain.Tripulante;
import mz.maputobustrackerModule.domain.Ponto;
import mz.maputobustrackerModule.domain.Rota;
import mz.maputobustrackerModule.domain.Viagem;
import mz.maputobustrackerModule.domain.util.LibraryClass;

/**
 * Created by Hawkingg on 26/06/2016.
 */
public class StatusActivity extends AppCompatActivity implements ValueEventListener {
    private TextView txtCodImei;
    private TextView txtNomeTrip;
    private TextView txtEmViagem;
    private TextView txtLatitude;
    private TextView txtAutocarroAssociado;
    private TextView txtVelocidade;
    private TextView txtProximaParagem;
    private TextView txtLongitude;
    private ProgressDialog progressDialog;
    private ArrayList<Viagem> viagens;
    private HashMap<String,Viagem> mapaViagens;
    private Tripulante op;
    private Rota selectedRoute;
    private HashMap<String,Associacao>mapaAssociacoes;
    private ArrayList<Associacao> associacoes;
    private Viagem autocarroAssociado;
    private Ponto origem;
    private Ponto destino;
    private static final LatLng defaultLocation = new LatLng(-25.966308, 32.562239);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Intent intent = getIntent();
        op = (Tripulante) intent.getSerializableExtra("operador");
        origem =(Ponto) intent.getSerializableExtra("porigem");
        destino =(Ponto) intent.getSerializableExtra("pdestino");
        selectedRoute = (Rota) intent.getSerializableExtra("RotaSelecionada");
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Buscando Informação, Por favor aguarde...");
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        progressDialog.setCancelable(false);
        txtAutocarroAssociado = (TextView) findViewById(R.id.txtAutocarroAssociado);

        ScrollView sv = (ScrollView) findViewById(R.id.menuItens2);
        sv.scrollTo(0, 100);

        LibraryClass.getFirebase().child("Associacoes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                associacoes = new ArrayList<Associacao>();
                mapaAssociacoes = new HashMap<String, Associacao>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    if (postSnapshot.getValue(Associacao.class) != null) {
                        Associacao post = postSnapshot.getValue(Associacao.class);
                        associacoes.add(post);
                        mapaAssociacoes.put(post.getIdDispositivo(),post);
                    }
                }
                if(mapaAssociacoes.get(getIMEI()) != null )
                {
                    txtAutocarroAssociado.setText(mapaAssociacoes.get(getIMEI()).getCodAutocarro());
                }
                else
                {
                    txtAutocarroAssociado.setText("Não");
                }
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
        LibraryClass.getFirebase().child("Viagens").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mapaViagens = new HashMap<String, Viagem>();
                viagens = new ArrayList<Viagem>();
                System.out.println("There are" + dataSnapshot.getChildrenCount() + " Viagens");
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    if (postSnapshot.getValue(Viagem.class) != null && selectedRoute != null) {
                        Viagem post = postSnapshot.getValue(Viagem.class);
                        if (post.getCod_rota().equalsIgnoreCase(selectedRoute.getId())) {
                            mapaViagens.put(post.getId(), post);
                            viagens.add(post);
                        }
                    }
                }
                txtCodImei = (TextView) findViewById(R.id.txtCodImei);
                txtNomeTrip = (TextView) findViewById(R.id.txtNomeTripulante);
                txtEmViagem = (TextView) findViewById(R.id.txtEmViagem);
                txtLatitude = (TextView) findViewById(R.id.txtLatitude);
                txtLongitude = (TextView) findViewById(R.id.txtLongitude);
                txtVelocidade = (TextView) findViewById(R.id.txtVelocidade);
                txtProximaParagem =(TextView) findViewById(R.id.txtProximaparagem);

                if(mapaViagens.get(getIMEI()) == null)
                {
                    txtCodImei.setText(getIMEI());
                    txtNomeTrip.setText(op.getName());
                    txtEmViagem.setText("Não");
                    txtLatitude.setText(""+defaultLocation.latitude);
                    txtLongitude.setText(""+defaultLocation.longitude);
                    txtVelocidade.setText("0 km/h");
                    txtProximaParagem.setText("Sem destino");
                }else
                {
                    txtCodImei.setText(mapaViagens.get(getIMEI()).getId());
                    txtNomeTrip.setText(op.getName());
                    txtEmViagem.setText("Sim");
                    txtVelocidade.setText(mapaViagens.get(getIMEI()).getVelocidade());
                    txtProximaParagem.setText(mapaViagens.get(getIMEI()).getProximaParagem());
                    txtLatitude.setText(""+mapaViagens.get(getIMEI()).getLatitude());
                    txtLongitude.setText(""+mapaViagens.get(getIMEI()).getLongitude());
                    LibraryClass.getFirebase().child("Rotas").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Rota rota = postSnapshot.getValue(Rota.class);
                            if(rota.getId().equals(mapaViagens.get(getIMEI()).getCod_rota())){
                            selectedRoute = rota;
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("The read failed: " + databaseError.getMessage());
                    }
                });
                }
                progressDialog.dismiss();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });
    }
    public void callMap(View view)
    {
        Intent intent = new Intent(StatusActivity.this,MapsActivity.class);
        intent.putExtra("RotaSelecionada",selectedRoute);
        intent.putExtra("porigem",origem);
        intent.putExtra("pdestino",destino);
        intent.putExtra("operador",op);
        startActivity(intent);
    }
    private void fetchAssociacoes() {

    }
    public String getIMEI()
    {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return   telephonyManager.getDeviceId();
    }
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

    }
    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if( keyCode== KeyEvent.KEYCODE_BACK)
        {
            this.finish();
        }
        return super.onKeyDown(keyCode, event);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
