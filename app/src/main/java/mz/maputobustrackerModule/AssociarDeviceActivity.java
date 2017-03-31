package mz.maputobustrackerModule;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mz.maputobustrackerModule.domain.Associacao;
import mz.maputobustrackerModule.domain.Autocarro;
import mz.maputobustrackerModule.domain.Tripulante;
import mz.maputobustrackerModule.domain.Ponto;
import mz.maputobustrackerModule.domain.Rota;
import mz.maputobustrackerModule.domain.Viagem;
import mz.maputobustrackerModule.domain.util.LibraryClass;

/**
 * Created by Hawkingg on 27/06/2016.
 */
public class AssociarDeviceActivity extends AppCompatActivity {

    private TextView txtId;
    private Spinner spAutocarros;
    private Spinner spRotas;
    private ProgressDialog progressDialog;
    private Autocarro autocarroAssociado;
    private ArrayList<Rota> rotas;
    private ArrayList<Autocarro> autocarros;
    private Viagem vg;
    private  Rota rt;
    private Runnable timerRunnable;
    private Handler handler;
    public static List<LatLng> polyRoute = new ArrayList<>();
    public static List<LatLng> routeBZ = new ArrayList<>();
    public static List<LatLng> routeZB = new ArrayList<>();
    private Autocarro at;
    private ArrayList<LatLng> pontosfix;
    private int inicio;
    private int actual;
    private int fim;
    private boolean estaassociadoA = false;
    private boolean estaassociadoB = false;
    private boolean firstTime = true;
    private Rota selectedRota;
    private Autocarro selectedAutocarro;
    private Button btnIniciar;
    private Button btnParar;
    private ProgressBar progressBar;
    private Switch swtEspecificar;
    private Double distanciaMaxima;
    private Double distanciaminima;
    private Double tempoEstimado;
    private Spinner spOrigem;
    private Spinner spDestino;
    private Snackbar snackbar;
    private boolean iniciouServico =false;
    private Button btnRedefinir;
    private ArrayList<Ponto> paragens;
    private ArrayList<Ponto> paragensOg;
    private ArrayList<Ponto> fixOrigemPontosDest;
    private ArrayList<Ponto> fixOrigemPontosOrig;
    private HashMap<String,Viagem> mapaViagens;
    private HashMap<String,Autocarro> mapaAutocarros;
    private HashMap<String,Associacao>mapaAssociacoes;
    private ArrayList<Associacao> associacoes;
    private Ponto origem;
    public boolean selecionouOrigem =false;
    public boolean selecionouDestino=false;
    private Ponto destino ;
    private boolean emviagem =true;
    private boolean associado = false;
    private Ponto inicioParagem;
    private Ponto actualParagem;
    private Ponto pntOgr;
    private Tripulante op;
    private Associacao associacao;
    private Button btnAssociar;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regdevice);
        Intent intent = getIntent();
        op = (Tripulante) intent.getSerializableExtra("operador");
        spAutocarros = (Spinner) findViewById(R.id.spAutocarros);
        spRotas = (Spinner) findViewById(R.id.spRotas);
        swtEspecificar = (Switch) findViewById(R.id.swtEspecificar);
        spOrigem = (Spinner) findViewById(R.id.spOrigem);
        spDestino = (Spinner) findViewById(R.id.spDestino);
        spOrigem.setEnabled(false);
        spDestino.setEnabled(false);
        spRotas.setEnabled(false);
        paragens = new ArrayList<>();
        paragensOg = new ArrayList<>();
        fixOrigemPontosDest = new ArrayList<>();
        fixOrigemPontosOrig =  new ArrayList<>();
        btnIniciar = (Button) findViewById(R.id.btnAssociar);
        btnRedefinir = (Button) findViewById(R.id.btnRedefinir);
        btnAssociar = (Button) findViewById(R.id.btnRedefinir);
        btnAssociar.setText("Associar");
        pntOgr = new Ponto();
        pntOgr.setNome("Escolha uma opção");
        pntOgr.setId("0");
        btnIniciar.setEnabled(false);
        btnParar = (Button) findViewById(R.id.btnPararServiço);
        btnParar.setEnabled(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Buscando Informação, Por favor aguarde...");
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        progressDialog.setCancelable(false);
        rt = new Rota();
        rt.setName("Escolha uma opção");
        rt.setId("0");
        at = new Autocarro();
        at.setNome("Escolha uma opção");
        at.setId("0");
        txtId = (TextView) findViewById(R.id.txtIMEI);
        txtId.setText(getIMEI());
        ScrollView sv = (ScrollView) findViewById(R.id.menuItens);
        sv.scrollTo(0, 100);
        //////////////////////
        LibraryClass.getFirebase().child("Viagens").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mapaViagens = new HashMap<String, Viagem>();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    if (dataSnapshot.getValue(Viagem.class) != null) {
                        Viagem post = dataSnapshot.getValue(Viagem.class);
                        mapaViagens.put(post.getCod_autocarro(), post);
                    }
                }
            }
                @Override
                public void onCancelled(DatabaseError firebaseError) {
                    System.out.println("The read failed: " + firebaseError.getMessage());
                }
            });
        LibraryClass.getFirebase().child("Autocarros").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                autocarros = new ArrayList<Autocarro>();
                mapaAutocarros = new HashMap<String, Autocarro>();
                autocarros.add(at);
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    if (postSnapshot.getValue(Autocarro.class) != null) {
                        Autocarro post = postSnapshot.getValue(Autocarro.class);
                        if(mapaViagens.get(post.getId()) == null){
                            autocarros.add(post);
                            mapaAutocarros.put(post.getId(),post);
                        }
                    }
                    if (spAutocarros != null) {
                        spAutocarros.setAdapter(new ArrayAdapter<Autocarro>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, autocarros));
                    }
                }
                fetchAssociacoes();
                progressDialog.dismiss();
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
        LibraryClass.getFirebase().child("Rotas").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                rotas = new ArrayList<Rota>();
                rotas.add(rt);
                System.out.println("There are " + snapshot.getChildrenCount() + " Rotas");
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Rota post = postSnapshot.getValue(Rota.class);
                    System.out.println(post.getName() + " - " + post.getDistancia());
                    rotas.add(post);
                }
                if (spRotas != null) {
                    spRotas.setAdapter(new ArrayAdapter<Rota>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, rotas));
                }
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
        spAutocarros.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                    selectedAutocarro = (Autocarro) parentView.getItemAtPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
        spRotas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    selectedRota = (Rota) parentView.getItemAtPosition(position);
                    btnIniciar.setEnabled(true);
                    btnRedefinir.setEnabled(true);
                    firstTime =true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
        swtEspecificar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if(iniciouServico == false) {
                    if (buttonView.isChecked()) {
                        progressDialog.setMessage("Processando Informação, Por favor aguarde...");
                        progressDialog.setIndeterminate(true);
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        btnIniciar.setEnabled(false);
                        btnParar.setEnabled(false);
                        spOrigem.setEnabled(true);
                        Rota rotaX = (Rota) spRotas.getSelectedItem();
                        Autocarro atX = (Autocarro) spAutocarros.getSelectedItem();
                        if (atX.getNome().equals(at.getNome())) {
                            progressDialog.dismiss();
                            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                            snackbar = Snackbar.make(progressBar, "Por favor, especifique um autocarro", Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                            swtEspecificar.setChecked(false);
                        } else if (rotaX.getName().equals(rt.getName())) {
                            progressDialog.dismiss();
                            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                            snackbar = Snackbar.make(progressBar, "Por favor, especifique uma rota", Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                            swtEspecificar.setChecked(false);
                        } else {
                            fetchparagens();
                        }
                    } else {
                        firstTime = true;
                        origem = null;
                        destino = null;
                        spOrigem.setAdapter(null);
                        spDestino.setAdapter(null);
                        selecionouOrigem = false;
                        selecionouDestino = false;
                    }
                }
            }
        });
        spOrigem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selecionouOrigem = false;
                selecionouDestino = false;
                if (firstTime == true) {
                    firstTime = false;
                } else {
                    origem = (Ponto) spOrigem.getSelectedItem();
                    selecionouOrigem = true;
                    Double distPointMax1 = CalculationByDistance(paragensOg.get(0).getLatlng(), origem.getLatlng());
                    ArrayList<Ponto> filtredListaParagens1 = new ArrayList<>();
                    filtredListaParagens1.add(pntOgr);
                    for (Ponto pont : paragensOg) {
                        Double refParagDst = CalculationByDistance(paragensOg.get(0).getLatlng(), pont.getLatlng());
                        if (refParagDst > distPointMax1) {
                            filtredListaParagens1.add(pont);
                        }
                    }
                    if (filtredListaParagens1.size() <= 1) {
                        progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                        snackbar = Snackbar.make(progressBar, "Escolha um ponto de origem que não seja o último da lista", Snackbar.LENGTH_LONG);
                        View snackBarView = snackbar.getView();
                        snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                        snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                        snackbar.show();
                    } else {
                        paragens = filtredListaParagens1;
                        spOrigem.setEnabled(false);
                        spDestino.setEnabled(true);
                        firstTime = true;
                        spDestino.setSelection(1, false);
                        spDestino.setAdapter(new ArrayAdapter<Ponto>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, paragens));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
        spDestino.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selecionouDestino = false;
                if (firstTime == true) {
                    firstTime = false;
                } else {
                    destino = (Ponto) spDestino.getSelectedItem();
                    selecionouDestino = true;
                    spDestino.setEnabled(false);
                    btnIniciar.setEnabled(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
    }
    private void fetchAssociacoes() {
        LibraryClass.getFirebase().child("Associacoes").addListenerForSingleValueEvent(new ValueEventListener() {
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
                    int position =0;
                    associado =true;
                    for(int i=0; i<autocarros.size();i++)
                    {
                        if(autocarros.get(i).getId().equals(mapaAssociacoes.get(getIMEI()).getCodAutocarro()))
                        {
                            position =i;
                            autocarroAssociado = autocarros.get(i);
                        }
                    }
                    spAutocarros.setSelection(position,true);
                    spAutocarros.setEnabled(false);
                    btnIniciar.setEnabled(true);
                    spRotas.setEnabled(true);
                    btnAssociar.setText("Desassociar");
                }
                else if (mapaAssociacoes.get(getIMEI()) == null)
                {
                    spRotas.setEnabled(false);
                    btnAssociar.setText("Associar");
                    btnIniciar.setEnabled(false);
                    associado = false;
                }
                progressDialog.dismiss();
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void fetchparagens() {
        LibraryClass.getFirebase().child("Paragens").child(selectedRota.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                paragens = new ArrayList<Ponto>();
                paragensOg = new ArrayList<Ponto>();
                fixOrigemPontosDest = new ArrayList<Ponto>();
                fixOrigemPontosOrig = new ArrayList<Ponto>();
                paragens.add(pntOgr);
                fixOrigemPontosDest.add(pntOgr);
                fixOrigemPontosOrig.add(pntOgr);
                System.out.println("There are " + snapshot.getChildrenCount() + " Rotas");
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Ponto post = postSnapshot.getValue(Ponto.class);
                    System.out.println(post.getNome() + " - " + post.getId());
                    paragens.add(post);
                    paragensOg.add(post);
                    fixOrigemPontosDest.add(post);
                    fixOrigemPontosOrig.add(post);
                }
                origem = paragensOg.get(0);
                destino = paragensOg.get(paragensOg.size()-1);
                if (spOrigem != null && spDestino !=null && !paragens.isEmpty()) {
                    fixOrigemPontosDest.remove(1);
                    fixOrigemPontosOrig.remove(fixOrigemPontosOrig.size() - 1);
                    spOrigem.setAdapter(new ArrayAdapter<Ponto>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, fixOrigemPontosOrig));
                    spDestino.setAdapter(new ArrayAdapter<Ponto>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, fixOrigemPontosDest));
                    progressDialog.dismiss();
                } else {
                    progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                    snackbar = Snackbar.make(progressBar, "Infelizmente o itinerário da rota selecionada está indisponível. Pf volte a tentar mais tarde", Snackbar.LENGTH_LONG);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                    snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                    snackbar.show();
                }
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }
    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius=6371;//radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult= Radius*c;
        double km=valueResult/1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec =  Integer.valueOf(newFormat.format(km));
        double meter=valueResult%1000;
        int  meterInDec= Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value",""+valueResult+"   KM  "+kmInDec+" Meter   "+meterInDec);
        return Radius * c;
    }

    public void resetChoose(View view) {
        progressDialog.setMessage("Processando Informação, Por favor aguarde...");
        progressDialog.show();
        firstTime = true;
        origem = null;
        destino = null;
        iniciouServico = false;
        selecionouOrigem = false;
        selecionouDestino = false;
        selecionouDestino = false;
        swtEspecificar.setChecked(false);
        btnRedefinir.setEnabled(false);
        spAutocarros.setAdapter(new ArrayAdapter<Autocarro>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, autocarros));
        spRotas.setAdapter(new ArrayAdapter<Rota>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, rotas));
        spOrigem.setAdapter(null);
        spDestino.setAdapter(null);
        spAutocarros.setEnabled(true);
        btnAssociar.setText("Associar");
        fetchAssociacoes();
    }
    public String getIMEI()
    {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return   telephonyManager.getDeviceId();
    }
    public void iniciarServico(View view)
    {
        Rota rotaX = (Rota) spRotas.getSelectedItem();
        Autocarro atX = (Autocarro) spAutocarros.getSelectedItem();
        if(atX == at || atX == null)
        {
            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
            snackbar= Snackbar.make(progressBar,"Por favor, especifique um autocarro",Snackbar.LENGTH_LONG);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
            snackbar.show();
        }
        else if(rotaX == rt || rotaX == null)
        {
            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
            snackbar= Snackbar.make(progressBar,"Por favor, especifique uma rota",Snackbar.LENGTH_LONG);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
            snackbar.show();
        }
        else
        {
            if(!swtEspecificar.isChecked())
            {
                LibraryClass.getFirebase().child("Paragens").child(selectedRota.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        paragens = new ArrayList<Ponto>();
                        paragensOg = new ArrayList<Ponto>();
                        fixOrigemPontosDest = new ArrayList<Ponto>();
                        fixOrigemPontosOrig = new ArrayList<Ponto>();
                        paragens.add(pntOgr);
                        System.out.println("There are " + snapshot.getChildrenCount() + " Rotas");
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Ponto post = postSnapshot.getValue(Ponto.class);
                            System.out.println(post.getNome() + " - " + post.getId());
                            paragens.add(post);
                            paragensOg.add(post);
                            fixOrigemPontosDest.add(post);
                            fixOrigemPontosOrig.add(post);
                        }
                        if (!paragens.isEmpty()) {
                            origem = paragensOg.get(0);
                            destino = paragensOg.get(paragensOg.size()-1);
                            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                            snackbar= Snackbar.make(progressBar,"Iniciou Serviço de Localização com sucesso",Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                            btnIniciar.setEnabled(false);
                            btnParar.setEnabled(true);
                            btnRedefinir.setEnabled(false);
                            iniciouServico = true;
                            startServiceSimulator();
                        } else {
                            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                            snackbar=Snackbar.make(progressBar, "Infelizmente o itinerário da rota selecionada está indisponível. Pf volte a tentar mais tarde", Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });

            }else
            {
                origem = (Ponto) spOrigem.getSelectedItem();
                destino = (Ponto) spDestino.getSelectedItem();
                System.out.println("ORIGEM "+origem.getNome());
                System.out.println("DESTINO "+destino.getNome());
                if(origem == pntOgr)
                {
                    progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                    snackbar=Snackbar.make(progressBar,"Por favor, especifique um ponto de origem",Snackbar.LENGTH_LONG);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                    snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                    snackbar.show();
                }
                else if (destino == pntOgr)
                {
                    progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                    snackbar= Snackbar.make(progressBar,"Por favor, especifique um ponto de destino",Snackbar.LENGTH_LONG);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                    snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                    snackbar.show();
                }else
                {
                    progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                    snackbar= Snackbar.make(progressBar,"Iniciou Serviço de Localização com sucesso",Snackbar.LENGTH_LONG);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                    snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                    snackbar.show();
                    btnIniciar.setEnabled(false);
                    btnParar.setEnabled(true);
                    btnRedefinir.setEnabled(false);
                    iniciouServico = true;
                    startServiceSimulator();
                }
            }

        }
    }
    public void associar(View view){
        if(selectedAutocarro == null )
        {
            progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
            snackbar=Snackbar.make(progressBar,"Por favor, especifique um autocarro",Snackbar.LENGTH_LONG);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
            snackbar.show();
        }
        else
        {
            if(selectedAutocarro.getNome().equals(at.getNome()))
            {
                progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                snackbar=Snackbar.make(progressBar,"Por favor, especifique um autocarro",Snackbar.LENGTH_LONG);
                View snackBarView = snackbar.getView();
                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                snackbar.show();
            }
            else {
                estaassociadoA = false;
                estaassociadoB = false;
                if (btnAssociar.getText().equals("Associar")) {
                    progressDialog.setMessage("Associando Autocarro ao dispositivo...por favor aguarde");
                    LibraryClass.getFirebase().child("Associacoes").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            associacoes = new ArrayList<Associacao>();
                            mapaAssociacoes = new HashMap<String, Associacao>();
                            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                if (postSnapshot.getValue(Associacao.class) != null) {
                                    Associacao post = postSnapshot.getValue(Associacao.class);
                                    associacoes.add(post);
                                    mapaAssociacoes.put(post.getIdDispositivo(), post);
                                }
                            }
                            Associacao associacoaA = new Associacao();
                            if(!associacoes.isEmpty()) {
                                for (Associacao as : associacoes) {
                                    if (as.getCodAutocarro().equals(selectedAutocarro.getId())) {
                                        estaassociadoA = true;
                                        System.out.println("AUTOCARRO EST}A ASSOCIADO>"+as.getCodAutocarro());
                                        associacoaA = as;
                                    }
                                }
                                int position = 0;
                                if(mapaAssociacoes.get(getIMEI())!= null) {
                                    for (int i = 0; i < autocarros.size(); i++) {
                                        if (autocarros.get(i).getId().equals(mapaAssociacoes.get(getIMEI()).getCodAutocarro())) {
                                            position = i;
                                            autocarroAssociado = autocarros.get(i);
                                            System.out.println("Dispositivo associado ao "+autocarroAssociado.getNome());
                                            estaassociadoB = true;
                                        }
                                    }
                                }
                            }
                            if (estaassociadoA == true) {

                                progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                                snackbar= Snackbar.make(progressBar, " O "+selectedAutocarro.getNome() +" já está associado ao dispositivo GPS com o id: "+associacoaA.getIdDispositivo() +".", Snackbar.LENGTH_LONG);
                                View snackBarView = snackbar.getView();
                                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                snackbar.show();
                            }
                            if(estaassociadoB == true)
                            {
                                progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                                snackbar =Snackbar.make(progressBar, " O actual dispositivo GPS"+ getIMEI()+" já está associado ao autocarro "+ autocarroAssociado.getNome()+".", Snackbar.LENGTH_LONG);
                                View snackBarView = snackbar.getView();
                                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                snackbar.show();
                            }
                            else if(estaassociadoA == false && estaassociadoB == false) {
                                associacao = new Associacao();
                                associacao.setCodAutocarro(selectedAutocarro.getId());
                                associacao.setIdDispositivo(getIMEI());
                                LibraryClass.getFirebase().child("Associacoes").child(getIMEI()).setValue(associacao);
                                spAutocarros.setEnabled(false);
                                btnAssociar.setText("Desassociar");
                                btnIniciar.setEnabled(true);
                                spRotas.setEnabled(true);
                                progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                                snackbar= Snackbar.make(progressBar, "O "+selectedAutocarro.getNome()+" foi associado ao dispositivo "+getIMEI()+" com sucesso.", Snackbar.LENGTH_LONG);
                                View snackBarView = snackbar.getView();
                                snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                                snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                snackbar.show();
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError firebaseError) {
                            System.out.println("The read failed: " + firebaseError.getMessage());
                        }
                    });
                }else if(btnAssociar.getText().equals("Desassociar"))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AssociarDeviceActivity.this);
                    builder.setMessage("Confirma que deseja desassociar o dispositivo do " + selectedAutocarro.getNome()+" ?")
                            .setCancelable(true)
                            .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    LibraryClass.getFirebase().child("Associacoes").child(getIMEI()).removeValue();
                                    spAutocarros.setEnabled(true);
                                    btnIniciar.setEnabled(false);
                                    spRotas.setEnabled(false);
                                    btnAssociar.setText("Associar");
                                    spAutocarros.setAdapter(new ArrayAdapter<Autocarro>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, autocarros));
                                    progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                                    snackbar= Snackbar.make(progressBar, "O "+selectedAutocarro.getNome()+" foi desassociado do dispositivo "+getIMEI()+" com sucesso.", Snackbar.LENGTH_LONG);
                                    View snackBarView = snackbar.getView();
                                    snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                                    snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                                    snackbar.show();
                                }
                            })
                            .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();


                }
            }
        }


    }
    public void callStatus(View view)
    {
        Intent intent = new Intent(AssociarDeviceActivity.this,StatusActivity.class);
        intent.putExtra("RotaSelecionada",selectedRota);
        System.out.println("SELECTED ROTA ASSOCIAR: "+selectedRota.getId());
        intent.putExtra("porigem",origem);
        intent.putExtra("pdestino",destino);
        intent.putExtra("operador",op);
        startActivity(intent);
    }

    public void pararServico(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(AssociarDeviceActivity.this);
        builder.setMessage("Confirma que deseja parar o Serviço GPS?")
                .setCancelable(true)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        progressBar = (ProgressBar) findViewById(R.id.ProgressRegDevice);
                        progressDialog.setMessage("Parando serviço de localização .....");
                        progressDialog.show();
                        handler.removeCallbacks(timerRunnable);
                        LibraryClass.getFirebase().child("Viagens").child(selectedAutocarro.getId()).removeValue();
                        progressDialog.dismiss();
                        if(emviagem == true) {
                            snackbar=Snackbar.make(progressBar, "Parou o Serviço GPS com sucesso!", Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                        }
                        else
                        {
                            snackbar=Snackbar.make(progressBar, "Rota concluida com sucesso. Serviço de Localização Finalizado!", Snackbar.LENGTH_LONG);
                            View snackBarView = snackbar.getView();
                            snackBarView.setBackgroundColor(Color.parseColor("#ff21ab29")); // snackbar background color
                            snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                            snackbar.show();
                        }
                        firstTime = true;
                        origem = null;
                        destino = null;
                        iniciouServico = false;
                        selecionouOrigem = false;
                        selecionouDestino = false;
                        selecionouDestino = false;
                        swtEspecificar.setChecked(false);
                        btnRedefinir.setEnabled(false);
                        spAutocarros.setAdapter(new ArrayAdapter<Autocarro>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, autocarros));
                        spRotas.setAdapter(new ArrayAdapter<Rota>(AssociarDeviceActivity.this, android.R.layout.simple_list_item_1, rotas));
                        spOrigem.setAdapter(null);
                        spDestino.setAdapter(null);
                        btnParar.setEnabled(false);
                        btnAssociar.setText("Associar");
                        fetchAssociacoes();
                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if( keyCode== KeyEvent.KEYCODE_BACK)
        {
            if(iniciouServico == false) {
                this.finish();
            }else
            {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void startServiceSimulator()
    {
        polyRoute = new ArrayList<>();
        routeBZ = new ArrayList<>();
        routeZB= new ArrayList<>();

//------------praca - av nacoes unidas
        routeBZ.add(new LatLng(-25.971316, 32.564954));
        routeBZ.add(new LatLng(-25.971248, 32.565061));
        routeBZ.add(new LatLng(-25.971147, 32.565200));
        routeBZ.add(new LatLng(-25.971108, 32.565318));
        routeBZ.add(new LatLng(-25.971031, 32.565474));
        routeBZ.add(new LatLng(-25.970949, 32.565587));
        routeBZ.add(new LatLng(-25.970872, 32.565662));
        routeBZ.add(new LatLng(-25.970773, 32.565734));
        routeBZ.add(new LatLng(-25.970679, 32.565806));
        routeBZ.add(new LatLng(-25.970560, 32.565905));
        routeBZ.add(new LatLng(-25.970468, 32.565964));
        routeBZ.add(new LatLng(-25.970408, 32.566007));
        routeBZ.add(new LatLng(-25.970340, 32.566063));
        routeBZ.add(new LatLng(-25.970277, 32.566122));
        routeBZ.add(new LatLng(-25.970209, 32.566170));
        routeBZ.add(new LatLng(-25.970141, 32.566229));
        routeBZ.add(new LatLng(-25.970047, 32.566264));
        routeBZ.add(new LatLng(-25.969900, 32.565998));
        routeBZ.add(new LatLng(-25.969722, 32.565714));
        routeBZ.add(new LatLng(-25.969570, 32.565491));
        routeBZ.add(new LatLng(-25.969413, 32.565231));
        routeBZ.add(new LatLng(-25.969261, 32.564982));
        routeBZ.add(new LatLng(-25.969066, 32.564601));
        routeBZ.add(new LatLng(-25.968815, 32.564158));
        routeBZ.add(new LatLng(-25.968395, 32.563431));
        routeBZ.add(new LatLng(-25.968154, 32.562991));
        routeBZ.add(new LatLng(-25.967966, 32.562674));
        routeBZ.add(new LatLng(-25.967797, 32.562387));
        routeBZ.add(new LatLng(-25.967556, 32.561969));
        routeBZ.add(new LatLng(-25.967175, 32.561301));
        routeBZ.add(new LatLng(-25.966951, 32.560931));
        routeBZ.add(new LatLng(-25.966621, 32.560344));
        routeBZ.add(new LatLng(-25.966380, 32.559971));
        routeBZ.add(new LatLng(-25.965825, 32.559472));
        routeBZ.add(new LatLng(-25.965285, 32.559105));
        routeBZ.add(new LatLng(-25.964798, 32.558724));
        routeBZ.add(new LatLng(-25.963168, 32.557544));
        routeBZ.add(new LatLng(-25.961789, 32.556611));
        routeBZ.add(new LatLng(-25.960323, 32.555570));
        routeBZ.add(new LatLng(-25.958809, 32.554508));
        routeBZ.add(new LatLng(-25.958066, 32.554090));
        routeBZ.add(new LatLng(-25.957670, 32.554294));
        routeBZ.add(new LatLng(-25.957523, 32.554435));
        routeBZ.add(new LatLng(-25.957398, 32.554263));
        routeBZ.add(new LatLng(-25.956433, 32.553072));
        routeBZ.add(new LatLng(-25.955594, 32.552278));
        routeBZ.add(new LatLng(-25.955034, 32.551774));
        routeBZ.add(new LatLng(-25.954967, 32.551709));
//-------------- nu brigada
        routeBZ.add(new LatLng(-25.954620, 32.551580));
        routeBZ.add(new LatLng(-25.954437, 32.551805));
        routeBZ.add(new LatLng(-25.954205, 32.552062));
        routeBZ.add(new LatLng(-25.953867, 32.552266));
        routeBZ.add(new LatLng(-25.953867, 32.552266));
        routeBZ.add(new LatLng(-25.953652, 32.552180));
        routeBZ.add(new LatLng(-25.953377, 32.552124));
        routeBZ.add(new LatLng(-25.953208, 32.551850));
        routeBZ.add(new LatLng(-25.953032, 32.551367));
        routeBZ.add(new LatLng(-25.952446, 32.550441));
        routeBZ.add(new LatLng(-25.952125, 32.550063));
        routeBZ.add(new LatLng(-25.951807, 32.549730));
        routeBZ.add(new LatLng(-25.951481, 32.549373));
        routeBZ.add(new LatLng(-25.951139, 32.549006));
        routeBZ.add(new LatLng(-25.950751, 32.548590));
        routeBZ.add(new LatLng(-25.949053, 32.546863));
        routeBZ.add(new LatLng(-25.947722, 32.545350));
        routeBZ.add(new LatLng(-25.946641, 32.544223));
        routeBZ.add(new LatLng(-25.944866, 32.541991));
        routeBZ.add(new LatLng(-25.944220, 32.541101));
        routeBZ.add(new LatLng(-25.944027, 32.540940));
        routeBZ.add(new LatLng(-25.943766, 32.541305));
        routeBZ.add(new LatLng(-25.943177, 32.541863));

//-------------- brigada junta
        routeBZ.add(new LatLng(-25.943100, 32.542013));
        routeBZ.add(new LatLng(-25.942743, 32.542507));
        routeBZ.add(new LatLng(-25.942357, 32.542925));
        routeBZ.add(new LatLng(-25.941768, 32.543483));
        routeBZ.add(new LatLng(-25.941199, 32.543998));
        routeBZ.add(new LatLng(-25.941199, 32.543998));
        routeBZ.add(new LatLng(-25.940794, 32.544395));
        routeBZ.add(new LatLng(-25.940041, 32.545060));
        routeBZ.add(new LatLng(-25.938854, 32.546187));
        routeBZ.add(new LatLng(-25.938044, 32.546970));
        routeBZ.add(new LatLng(-25.936934, 32.547968));
        routeBZ.add(new LatLng(-25.936403, 32.548547));
        routeBZ.add(new LatLng(-25.935728, 32.549201));
        routeBZ.add(new LatLng(-25.935149, 32.549727));
        routeBZ.add(new LatLng(-25.934802, 32.550081));
        routeBZ.add(new LatLng(-25.934638, 32.550210));
        routeBZ.add(new LatLng(-25.934161, 32.550501));
        routeBZ.add(new LatLng(-25.933949, 32.550909));
//---substituir paragem uem por junta

//retirar paragem jardim

//---------------- junta jardim
        routeBZ.add(new LatLng(-25.933032, 32.551724));
        routeBZ.add(new LatLng(-25.932723, 32.552067));
        routeBZ.add(new LatLng(-25.932473, 32.552261));
        routeBZ.add(new LatLng(-25.931836, 32.552873));
        routeBZ.add(new LatLng(-25.931334, 32.553345));
        routeBZ.add(new LatLng(-25.930523, 32.554032));
        routeBZ.add(new LatLng(-25.929635, 32.554837));
        routeBZ.add(new LatLng(-25.929027, 32.555427));
        routeBZ.add(new LatLng(-25.929027, 32.555427));
        routeBZ.add(new LatLng(-25.928535, 32.555921));
        routeBZ.add(new LatLng(-25.927821, 32.556543));
        routeBZ.add(new LatLng(-25.927317, 32.557141));

//-------------------------------------------- Jardim inhagoia
        routeBZ.add(new LatLng(-25.926958, 32.557412));
        routeBZ.add(new LatLng(-25.926500, 32.557804));
        routeBZ.add(new LatLng(-25.925983, 32.558313));
        routeBZ.add(new LatLng(-25.923999, 32.560135));
        routeBZ.add(new LatLng(-25.923459, 32.560618));
        routeBZ.add(new LatLng(-25.923459, 32.560618));
        routeBZ.add(new LatLng(-25.923157, 32.560925));
        routeBZ.add(new LatLng(-25.922940, 32.561097));

//----------------------------- ----- inhagoia 25 de Junho
        routeBZ.add(new LatLng(-25.922600, 32.561441));
        routeBZ.add(new LatLng(-25.922325, 32.561731));
        routeBZ.add(new LatLng(-25.921998, 32.561992));
        routeBZ.add(new LatLng(-25.921598, 32.562374));
        routeBZ.add(new LatLng(-25.920994, 32.562931));
        routeBZ.add(new LatLng(-25.920290, 32.563574));
        routeBZ.add(new LatLng(-25.919643, 32.564202));
        routeBZ.add(new LatLng(-25.919050, 32.564739));
        routeBZ.add(new LatLng(-25.918312, 32.565168));
        routeBZ.add(new LatLng(-25.917203, 32.565350));
        routeBZ.add(new LatLng(-25.915882, 32.565425));
        routeBZ.add(new LatLng(-25.914175, 32.565441));
        routeBZ.add(new LatLng(-25.912814, 32.565495));
        routeBZ.add(new LatLng(-25.911241, 32.565474));
        routeBZ.add(new LatLng(-25.910141, 32.565485));
        routeBZ.add(new LatLng(-25.909172, 32.565495));

//---- 25 de junho bagamoio
        routeBZ.add(new LatLng(-25.908630, 32.565533));
        routeBZ.add(new LatLng(-25.907906, 32.565581));
        routeBZ.add(new LatLng(-25.906956, 32.565538));
        routeBZ.add(new LatLng(-25.906039, 32.565608));
        routeBZ.add(new LatLng(-25.905160, 32.565608));
        routeBZ.add(new LatLng(-25.903978, 32.565356));
        routeBZ.add(new LatLng(-25.902424, 32.564895));
        routeBZ.add(new LatLng(-25.901034, 32.564434));
        routeBZ.add(new LatLng(-25.899828, 32.564101));
        routeBZ.add(new LatLng(-25.898737, 32.563715));
        routeBZ.add(new LatLng(-25.898177, 32.563600));
        routeBZ.add(new LatLng(-25.897612, 32.563573));
        routeBZ.add(new LatLng(-25.896685, 32.563541));
        routeBZ.add(new LatLng(-25.894600, 32.563638));
        routeBZ.add(new LatLng(-25.893644, 32.563649));
        routeBZ.add(new LatLng(-25.893098, 32.563756));



        //-------------------------------------------- bagamoio benfica

        routeBZ.add(new LatLng(-25.892073, 32.563976));
        routeBZ.add(new LatLng(-25.891551, 32.564126));
        routeBZ.add(new LatLng(-25.890722, 32.564416));
        routeBZ.add(new LatLng(-25.889902, 32.564684));
        routeBZ.add(new LatLng(-25.887422, 32.565499));
        routeBZ.add(new LatLng(-25.885714, 32.566089));
        routeBZ.add(new LatLng(-25.885173, 32.566282));
        routeBZ.add(new LatLng(-25.884583, 32.566528));
        //----------------------------------------------- benfica shopping


        routeBZ.add(new LatLng(-25.883894, 32.566829));
        routeBZ.add(new LatLng(-25.882933, 32.567338));
        routeBZ.add(new LatLng(-25.881950, 32.567846));
        routeBZ.add(new LatLng(-25.881698, 32.567950));
        routeBZ.add(new LatLng(-25.880998, 32.568073));
        routeBZ.add(new LatLng(-25.878710, 32.567311));
        routeBZ.add(new LatLng(-25.878100, 32.566964));
        routeBZ.add(new LatLng(-25.877569, 32.566707));
        routeBZ.add(new LatLng(-25.877168, 32.566498));
        routeBZ.add(new LatLng(-25.876840, 32.566359));
        routeBZ.add(new LatLng(-25.876444, 32.566171));
        routeBZ.add(new LatLng(-25.876222, 32.566129));
        //------------------------------------------------ shopping  Mercado Zimpeto
        routeBZ.add(new LatLng(-25.875797, 32.565860));
        routeBZ.add(new LatLng(-25.875229, 32.565623));
        routeBZ.add(new LatLng(-25.874833, 32.565479));
        routeBZ.add(new LatLng(-25.874133, 32.565377));
        routeBZ.add(new LatLng(-25.873544, 32.565404));
        routeBZ.add(new LatLng(-25.872873, 32.565458));
        routeBZ.add(new LatLng(-25.871927, 32.565549));
        routeBZ.add(new LatLng(-25.871140, 32.565640));
        routeBZ.add(new LatLng(-25.870353, 32.565726));
        routeBZ.add(new LatLng(-25.868948, 32.565855));
        routeBZ.add(new LatLng(-25.866679, 32.566091));
        routeBZ.add(new LatLng(-25.865038, 32.566241));
        routeBZ.add(new LatLng(-25.865183, 32.566241));
        routeBZ.add(new LatLng(-25.863605, 32.566380));
        routeBZ.add(new LatLng(-25.861800, 32.566627));
        routeBZ.add(new LatLng(-25.860139, 32.566681));
        routeBZ.add(new LatLng(-25.858295, 32.566917));
        routeBZ.add(new LatLng(-25.856519, 32.567089));
        routeBZ.add(new LatLng(-25.854723, 32.567293));
        routeBZ.add(new LatLng(-25.853449, 32.567787));
        routeBZ.add(new LatLng(-25.852097, 32.568345));
        routeBZ.add(new LatLng(-25.852097, 32.568345));
        routeBZ.add(new LatLng(-25.849712, 32.568484));
        routeBZ.add(new LatLng(-25.848727, 32.568479));
        routeBZ.add(new LatLng(-25.847723, 32.568522));
        routeBZ.add(new LatLng(-25.846535, 32.568543));
        routeBZ.add(new LatLng(-25.845321, 32.568590));
        routeBZ.add(new LatLng(-25.844307, 32.568622));
        routeBZ.add(new LatLng(-25.842743, 32.568676));
        routeBZ.add(new LatLng(-25.840590, 32.568730));
        routeBZ.add(new LatLng(-25.839373, 32.568741));
        routeBZ.add(new LatLng(-25.837451, 32.568827));
        routeBZ.add(new LatLng(-25.835684, 32.568881));
        routeBZ.add(new LatLng(-25.833685, 32.568913));
        routeBZ.add(new LatLng(-25.832525, 32.568998));
//--------------------------------------- Mercado Zimpeto Terminal Zimpeto

        routeBZ.add(new LatLng(-25.831743, 32.568999));
        routeBZ.add(new LatLng(-25.830951, 32.569224));
        routeBZ.add(new LatLng(-25.830082, 32.569557));
        routeBZ.add(new LatLng(-25.828827, 32.569879));
        routeBZ.add(new LatLng(-25.827552, 32.570147));
        routeBZ.add(new LatLng(-25.827581, 32.570404));
        routeBZ.add(new LatLng(-25.827726, 32.571959));
        routeBZ.add(new LatLng(-25.828218, 32.571873));
        routeBZ.add(new LatLng(-25.828933, 32.571884));
        routeBZ.add(new LatLng(-25.829387, 32.571895));
        routeBZ.add(new LatLng(-25.829609, 32.572689));
        routeBZ.add(new LatLng(-25.830043, 32.574481));
        routeBZ.add(new LatLng(-25.830435, 32.575711));
        routeBZ.add(new LatLng(-25.830672, 32.577062));
        routeBZ.add(new LatLng(-25.831063, 32.576993));
        routeBZ.add(new LatLng(-25.830966, 32.576322));
        routeBZ.add(new LatLng(-25.830846, 32.575601));
        routeBZ.add(new LatLng(-25.830672, 32.575198));
        routeBZ.add(new LatLng(-25.830474, 32.574576));



//Terminal Zimpeto - Mercado Zimpeto

        routeZB.add(new LatLng(-25.830474, 32.574576));
        routeZB.add(new LatLng(-25.830672, 32.575198));
        routeZB.add(new LatLng(-25.830846, 32.575601));
        routeZB.add(new LatLng(-25.830966, 32.576322));
        routeZB.add(new LatLng(-25.831063, 32.576993));
        routeZB.add(new LatLng(-25.830672, 32.577062));
        routeZB.add(new LatLng(-25.830435, 32.575711));
        routeZB.add(new LatLng(-25.829967, 32.574102));
        routeZB.add(new LatLng(-25.829769, 32.573313));
        routeZB.add(new LatLng(-25.829532, 32.572390));
        routeZB.add(new LatLng(-25.829281, 32.571859));
        routeZB.add( new LatLng(-25.828412, 32.571902));
        routeZB.add( new LatLng(-25.827697, 32.571940));
        routeZB.add(new LatLng(-25.827591, 32.571157));
        routeZB.add( new LatLng(-25.827548, 32.570422));
        routeZB.add(  new LatLng(-25.827871, 32.570105));
        routeZB.add( new LatLng(-25.828634, 32.569966));
        routeZB.add( new LatLng(-25.829716, 32.569724));
        routeZB.add( new LatLng(-25.830701, 32.569338));

//Mercado Zimpeto - Mabor
        routeZB.add( new LatLng(-25.831010, 32.569215));
        routeZB.add(new LatLng(-25.831362, 32.569107));
        routeZB.add( new LatLng(-25.832029, 32.569054));
        routeZB.add(new LatLng(-25.832599, 32.569011));
        routeZB.add(   new LatLng(-25.833458, 32.568973));
        routeZB.add(new LatLng(-25.834226, 32.568936));
        routeZB.add(  new LatLng(-25.835331, 32.568930));
        routeZB.add(new LatLng(-25.836640, 32.568909));
        routeZB.add(new LatLng(-25.836988, 32.568930));

//Mabor - Benfica

        routeZB.add(new LatLng(-25.837234, 32.568871));
        routeZB.add(new LatLng(-25.837634, 32.568866));
        routeZB.add(new LatLng(-25.837866, 32.568861));
        routeZB.add(new LatLng(-25.838240, 32.568856));
        routeZB.add(new LatLng(-25.838578, 32.568837));
        routeZB.add(new LatLng(-25.839109, 32.568813));
        routeZB.add(new LatLng(-25.839643, 32.568797));
        routeZB.add(new LatLng(-25.840094, 32.568789));
        routeZB.add(new LatLng(-25.840565, 32.568778));
        routeZB.add(new LatLng(-25.840922, 32.568759));
        routeZB.add(new LatLng(-25.841407, 32.568756));
        routeZB.add(new LatLng(-25.838751, 32.568744));
        routeZB.add( new LatLng(-25.840528, 32.568755));
        routeZB.add( new LatLng(-25.842421, 32.568723));
        routeZB.add(   new LatLng(-25.844091, 32.568637));
        routeZB.add(  new LatLng(-25.846688, 32.568605));
        routeZB.add(   new LatLng(-25.847963, 32.568573));
        routeZB.add(   new LatLng(-25.849353, 32.568508));
        routeZB.add(   new LatLng(-25.850860, 32.568465));
        routeZB.add(    new LatLng(-25.852820, 32.568165));
        routeZB.add(      new LatLng(-25.854933, 32.567322));
        routeZB.add(     new LatLng(-25.855497, 32.567289));
        routeZB.add(     new LatLng(-25.856342, 32.567214));
        routeZB.add(      new LatLng(-25.857482, 32.567096));
        routeZB.add(    new LatLng(-25.858259, 32.567016));
        routeZB.add(    new LatLng(-25.859427, 32.566855));
        routeZB.add(    new LatLng(-25.860816, 32.566784));
        routeZB.add(    new LatLng(-25.862148, 32.566618));
        routeZB.add(   new LatLng(-25.864504, 32.566355));
        routeZB.add(  new LatLng(-25.865677, 32.566269));
        routeZB.add(    new LatLng(-25.866145, 32.566269));
        routeZB.add(  new LatLng(-25.866768, 32.566215));
        routeZB.add(   new LatLng(-25.867352, 32.566162));
        routeZB.add( new LatLng(-25.868844, 32.565964));
        routeZB.add( new LatLng(-25.870639, 32.565770));
        routeZB.add(  new LatLng(-25.872309, 32.565631));
        routeZB.add( new LatLng(-25.873699, 32.565513));
        routeZB.add(  new LatLng(-25.874605, 32.565542));
        routeZB.add(  new LatLng(-25.875375, 32.565757));
        routeZB.add( new LatLng(-25.876193, 32.566240));
        routeZB.add( new LatLng(-25.876492, 32.566457));
        routeZB.add(  new LatLng(-25.876601, 32.566817));
        routeZB.add(   new LatLng(-25.876806, 32.567055));
        routeZB.add(  new LatLng(-25.877144, 32.566970));
        routeZB.add(  new LatLng(-25.877371, 32.566927));
        routeZB.add(  new LatLng(-25.878148, 32.567125));
        routeZB.add(  new LatLng(-25.880007, 32.568028));
        routeZB.add( new LatLng(-25.880658, 32.568146));
        routeZB.add( new LatLng(-25.881430, 32.568071));
        routeZB.add( new LatLng(-25.881985, 32.567840));
        routeZB.add( new LatLng(-25.882550, 32.567567));
        routeZB.add( new LatLng(-25.883453, 32.567084));
        routeZB.add(new LatLng(-25.884461, 32.566671));
        routeZB.add(new LatLng(-25.884934, 32.566499));
        routeZB.add( new LatLng(-25.884929, 32.566494));

// Benfica - Mercado George Dimitrov

        routeZB.add( new LatLng(-25.885407, 32.566365));
        routeZB.add(new LatLng(-25.885996, 32.566129));
        routeZB.add(new LatLng(-25.886430, 32.565984));
        routeZB.add(new LatLng(-25.886976, 32.565737));
        routeZB.add(new LatLng(-25.887743, 32.565539));
        routeZB.add(new LatLng(-25.888177, 32.565389));
        routeZB.add(new LatLng(-25.888978, 32.565089));
        routeZB.add( new LatLng(-25.889615, 32.564831));

//Mercado George Dimitrov - Bagamoyo

        routeZB.add(new LatLng(-25.890059, 32.564649));
        routeZB.add(new LatLng(-25.890629, 32.564466));
        routeZB.add(new LatLng(-25.891198, 32.564274));
        routeZB.add(new LatLng(-25.891980, 32.564075));
        routeZB.add(new LatLng(-25.892680, 32.563855));
        routeZB.add( new LatLng(-25.893032, 32.563823));
        routeZB.add( new LatLng(-25.893254, 32.563807));
        routeZB.add(new LatLng(-25.893365, 32.563801));
        routeZB.add(new LatLng(-25.894079, 32.563812));

//Bagamoyo - Bairo 25 de Junho

        routeZB.add(new LatLng(-25.894489, 32.563785));
        routeZB.add(new LatLng(-25.894861, 32.563785));
        routeZB.add(new LatLng(-25.895387, 32.563801));
        routeZB.add(new LatLng(-25.895773, 32.563780));
        routeZB.add(new LatLng(-25.896309, 32.563818));
        routeZB.add(new LatLng(-25.896811, 32.563796));
        routeZB.add(new LatLng(-25.897337, 32.563785));
        routeZB.add(new LatLng(-25.898085, 32.563801));
        routeZB.add(new LatLng(-25.898804, 32.563839));
        routeZB.add(new LatLng(-25.899368, 32.563978));
        routeZB.add( new LatLng(-25.899884, 32.564156));
        routeZB.add( new LatLng(-25.900671, 32.564392));
        routeZB.add(new LatLng(-25.901491, 32.564633));
        routeZB.add(new LatLng(-25.902017, 32.564778));
        routeZB.add(new LatLng(-25.902732, 32.565025));
        routeZB.add(new LatLng(-25.903537, 32.565298));
        routeZB.add( new LatLng(-25.904430, 32.565534));
        routeZB.add( new LatLng(-25.905144, 32.565641));
        routeZB.add( new LatLng(-25.905883, 32.565672));
        routeZB.add( new LatLng(-25.907733, 32.565597));
        routeZB.add( new LatLng(-25.909929, 32.565597));

// Bairo 25 de Junho - Bombas Jardim

        routeZB.add( new LatLng(-25.910884, 32.565506));
        routeZB.add(new LatLng(-25.911535, 32.565517));
        routeZB.add(new LatLng(-25.912438, 32.565485));
        routeZB.add(new LatLng(-25.913514, 32.565479));
        routeZB.add( new LatLng(-25.914233, 32.565447));
        routeZB.add( new LatLng(-25.915381, 32.565447));
        routeZB.add( new LatLng(-25.916090, 32.565447));
        routeZB.add( new LatLng(-25.917248, 32.565404));
        routeZB.add( new LatLng(-25.918117, 32.565270));
        routeZB.add(new LatLng(-25.918628, 32.565061));
        routeZB.add(new LatLng(-25.919506, 32.564401));
        routeZB.add( new LatLng(-25.920601, 32.563344));
        routeZB.add( new LatLng(-25.921547, 32.562513));
        routeZB.add(new LatLng(-25.922068, 32.562019));
        routeZB.add( new LatLng(-25.923096, 32.561016));
        routeZB.add( new LatLng(-25.924220, 32.560024));
        routeZB.add(new LatLng(-25.891256, 32.564295));

// Bombas Jardim -  Faculdade de Engenharias UEM

        routeZB.add(new LatLng(-25.925281, 32.559047));
        routeZB.add(new LatLng(-25.925716, 32.558613));
        routeZB.add( new LatLng(-25.926266, 32.558114));
        routeZB.add(new LatLng(-25.926700, 32.557663));
        routeZB.add(new LatLng(-25.927597, 32.556875));
        routeZB.add(new LatLng(-25.928388, 32.556129));
        routeZB.add(new LatLng(-25.929237, 32.555335));
        routeZB.add(new LatLng(-25.930154, 32.554520));
        routeZB.add(new LatLng(-25.930815, 32.553908));
        routeZB.add(new LatLng(-25.931418, 32.553367));
        routeZB.add(new LatLng(-25.931514, 32.553286));
        routeZB.add(new LatLng(-25.932740, 32.552144));
        routeZB.add(new LatLng(-25.933275, 32.551623));
        routeZB.add(new LatLng(-25.933956, 32.550969));
        routeZB.add(new LatLng(-25.934399, 32.550856));
        routeZB.add( new LatLng(-25.934679, 32.550792));
        routeZB.add( new LatLng(-25.934983, 32.550116));
        routeZB.add( new LatLng(-25.935432, 32.549671));
        routeZB.add( new LatLng(-25.935726, 32.549435));

//Faculdade de Engenharias UEM - Brigada Montada

        routeZB.add( new LatLng(-25.937492, 32.547718));
        routeZB.add(new LatLng(-25.939489, 32.545916));
        routeZB.add( new LatLng(-25.939653, 32.545669));
        routeZB.add( new LatLng(-25.940695, 32.544682));
        routeZB.add( new LatLng(-25.942441, 32.543008));
        routeZB.add( new LatLng(-25.943464, 32.541764));
        routeZB.add( new LatLng(-25.938341, 32.546892));
        routeZB.add( new LatLng(-25.943744, 32.541699));

// Brigada - Praça dos Trabalhadores
        routeZB.add(new LatLng(-25.944260, 32.542037));
        routeZB.add(new LatLng(-25.944795, 32.542423));
        routeZB.add(new LatLng(-25.945350, 32.542971));
        routeZB.add(new LatLng(-25.945770, 32.543405));
        routeZB.add(new LatLng(-25.946508, 32.544156));
        routeZB.add(new LatLng(-25.947265, 32.544961));
        routeZB.add(new LatLng(-25.947989, 32.545642));
        routeZB.add(new LatLng(-25.948712, 32.546393));
        routeZB.add(new LatLng(-25.948953, 32.546688));
        routeZB.add(new LatLng(-25.949566, 32.547348));
        routeZB.add(new LatLng(-25.950352, 32.548201));
        routeZB.add(new LatLng(-25.951288, 32.549172));
        routeZB.add(new LatLng(-25.952513, 32.550518));
        routeZB.add(new LatLng(-25.953049, 32.551468));
        routeZB.add(new LatLng(-25.953063, 32.552192));
        routeZB.add( new LatLng(-25.953015, 32.553024));
        routeZB.add(new LatLng(-25.953580, 32.553329));
        routeZB.add(new LatLng(-25.953961, 32.552890));
        routeZB.add(new LatLng(-25.954351, 32.552117));
        routeZB.add( new LatLng(-25.955171, 32.552149));
        routeZB.add( new LatLng(-25.956449, 32.553104));
        routeZB.add( new LatLng(-25.957313, 32.554107));
        routeZB.add( new LatLng(-25.957814, 32.554172));
        routeZB.add( new LatLng(-25.958620, 32.554354));
        routeZB.add( new LatLng(-25.959729, 32.555153));
        routeZB.add( new LatLng(-25.961167, 32.556157));
        routeZB.add(new LatLng(-25.962435, 32.557004));
        routeZB.add(new LatLng(-25.964229, 32.558308));
        routeZB.add(  new LatLng(-25.965763, 32.559343));
        routeZB.add( new LatLng(-25.966631, 32.560448));
        routeZB.add( new LatLng(-25.967369, 32.561655));
        routeZB.add( new LatLng(-25.968628, 32.563844));
        routeZB.add( new LatLng(-25.970031, 32.566258));
        routeZB.add( new LatLng(-25.970668, 32.566011));
        routeZB.add( new LatLng(-25.971907, 32.565410));
        routeZB.add( new LatLng(-25.971825, 32.564922));
        routeZB.add( new LatLng(-25.971454, 32.564863));
        routeZB.add(new LatLng(-25.971280, 32.564965));
//-----------------finish
        if(selectedRota.getId().equals("rota1"))
        {
            polyRoute = routeBZ;
        }else if(selectedRota.getId().equals("rota2"))
        {
            polyRoute = routeZB;
        }

        if(iniciouServico == true ) {
            emviagem = true;
            Double distPointMax = CalculationByDistance(origem.getLatlng(), destino.getLatlng());
            ArrayList<LatLng> filtredListaParagens = new ArrayList<>();
            pontosfix = new ArrayList<LatLng>();
            boolean encontrouOr =false;
            boolean encontrouDst = false;
            boolean stop =false;
            for(LatLng ponto: polyRoute)
            {
                if(origem.getLatlng().equals(ponto))
                {
                    encontrouOr=true;
                }
                else if(destino.getLatlng().equals(ponto))
                {
                    encontrouDst=true;
                }
                if(encontrouOr==true && encontrouDst==false && stop == false)
                {
                    filtredListaParagens.add(ponto);
                    pontosfix.add(ponto);
                } else if(encontrouOr==true && encontrouDst==true && stop == false)
                {
                    filtredListaParagens.add(ponto);
                    pontosfix.add(ponto);
                    stop =true;
                }
            }
            polyRoute = filtredListaParagens;
            LibraryClass.getFirebase().child("Viagens").removeValue();
            inicio = 0;
            actual = 1;
            Double distPointMax1 = CalculationByDistance(origem.getLatlng(), destino.getLatlng());
            ArrayList<Ponto> filtredListaParagens1 = new ArrayList<>();
            boolean encontrouOr1 = false;
            boolean encontrouDst1 = false;
            boolean stop1=false;
            for (Ponto pont : paragensOg) {

                if(origem.equals(pont))
                {
                    encontrouOr1=true;
                }
                else if(destino.equals(pont))
                {
                    encontrouDst1=true;
                }
                if(encontrouOr1==true && encontrouDst1==false && stop1 == false)
                {
                    filtredListaParagens1.add(pont);
                } else if(encontrouOr1==true && encontrouDst1==true && stop1 == false)
                {
                    filtredListaParagens1.add(pont);
                    stop1 =true;
                }
            }
            paragensOg = filtredListaParagens1;
            fim = paragensOg.size();
            paragensOg = filtredListaParagens1;
            inicioParagem = paragensOg.get(inicio);
            actualParagem = paragensOg.get(actual);

            Double dist = CalculationByDistance(polyRoute.get(0), inicioParagem.getLatlng());
            System.out.println("DISTANCIA> " + dist);
            System.out.println("TAMANHO DA LISTA> " + paragensOg.size());
            if (polyRoute.size() == 1) {
                inicioParagem = paragensOg.get(inicio);
                actualParagem = paragensOg.get(inicio);
            }
            if (dist > 0.1) {
                Collections.reverse(paragensOg);
                inicioParagem = paragensOg.get(inicio);
                actualParagem = paragensOg.get(actual);
            }
            distanciaMaxima = CalculationByDistance(inicioParagem.getLatlng(), actualParagem.getLatlng());
            distanciaminima = 0.0;
            vg = new Viagem();
            vg.setCod_rota(selectedRota.getId());
            vg.setId(getIMEI());
            vg.setCod_autocarro(selectedAutocarro.getId());
            vg.setDescricao(selectedAutocarro.getNome());
            vg.setVelocidade("40 km/h");
            handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final long duration = 400000;
            final Interpolator interpolator = new LinearOutSlowInInterpolator();
            handler.post(timerRunnable = new Runnable() {
                //Renew PolyRoute
                int i = 0;
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    double distance = 0;
                    float t = interpolator.getInterpolation((float) elapsed
                            / duration);
                    if (i < polyRoute.size()) {
                        vg.setLatitude(polyRoute.get(i).latitude);
                        vg.setLongitude(polyRoute.get(i).longitude);
                        LatLng dest = new LatLng(polyRoute.get(i).latitude, polyRoute.get(i).longitude);
                        distanciaminima = CalculationByDistance(inicioParagem.getLatlng(), dest);
                        System.out.println("Distancia Maxima>"+distanciaMaxima);
                        System.out.println("Distancia Minima>"+distanciaminima);
                        tempoEstimado = CalculationByDistance(dest, actualParagem.getLatlng());
                        double km = tempoEstimado / 1;
                        DecimalFormat newFormat = new DecimalFormat("####");
                        int kmInDec = Integer.valueOf(newFormat.format(km));
                        Double kmDo = kmInDec + 0.0;
                        tempoEstimado = kmDo / 40;
                        tempoEstimado = tempoEstimado * 60;
                        float[] distancea = new float[2];
                        Location.distanceBetween(vg.getLatitude(), vg.getLongitude(),
                                actualParagem.getLatitude(), actualParagem.getLongitude(), distancea);
                        if (tempoEstimado == 0) {
                            vg.setTempoEstChgada("menos de 1 minuto");
                            vg.setTempoReal(tempoEstimado);
                        } else {
                            vg.setTempoEstChgada(tempoEstimado.intValue() + " minutos");
                            vg.setTempoReal(tempoEstimado);
                        }
                        if (distanciaMaxima > distanciaminima) {
                            vg.setCod_proximaParagem(actualParagem.getId());
                            vg.setCod_anteriorParagem(inicioParagem.getId());
                            vg.setAnteriorParagem(inicioParagem.getNome());
                            vg.setProximaParagem(actualParagem.getNome());
                            vg.setChegouDestino(false);
                        } else {
                            inicio += 1;
                            actual += 1;
                            if (actual < fim) {
                                inicioParagem = actualParagem;
                                actualParagem = paragensOg.get(actual);
                                distanciaMaxima = CalculationByDistance(inicioParagem.getLatlng(), actualParagem.getLatlng());
                            } else {
                                emviagem = false;
                                i = polyRoute.size();
                                handler.removeCallbacks(timerRunnable);
                                //    LibraryClass.getFirebase().child("Viagens").child(autocarro.getId()).removeValue();
                            }
                            vg.setChegouDestino(true);
                            LibraryClass.getFirebase().child("Viagens").child(selectedAutocarro.getId()).setValue(vg);
                        }
                        LibraryClass.getFirebase().child("Viagens").child(selectedAutocarro.getId()).setValue(vg);
                        i++;
                        if (t < 1.0) {
                            // Post again 16ms later.
                            if (vg.getChegouDestino() == true) {
                                handler.postDelayed(this, 5000);
                            } else {
                                handler.postDelayed(this, 1000);
                            }
                        }
                    } else {
                        vg.setChegouDestino(true);
                        emviagem = false;
                        btnParar.performClick();
                        handler.removeCallbacks(timerRunnable);
                        LibraryClass.getFirebase().child("Viagens").child(selectedAutocarro.getId()).removeValue();
                    }
                }
            });
        }
        }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}