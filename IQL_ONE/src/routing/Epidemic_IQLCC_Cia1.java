/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.*;

import core.*;
import routing.*;
import static routing.Epidemic_IQLCC.repsproperty;
import routing.QL.*;

public abstract class Epidemic_IQLCC_Cia1 extends ActiveRouter implements QVDetectionEngine, BufferOccDetectionEngine {

    /**
     * Epidemic_IQLCC router's setting namespace ({@value})
     */
    public static final String Epidemic_IQLCC_NS = "eIQLCC";
    /**
     * minimum time for update new state (window) - setting id (@value)
     */
    public static final String STATE_UPDATE_INTERVAL_S = "stateInterval";
    /**
     * boltzmann value - setting id (@value)
     */
    public static final String BOLTZMANN_C_S = "boltzmannConsValue";
    /**
     * Congestion Threshold for state update - setting id (@value)
     */
    public static final String CTH_S = "CTH";
    /**
     * Non- Congestion Threshold for state update - setting id (@value)
     */
    public static final String NCTH_S = "NCTH";

    //Probability pemilian action
    public static final String PROBABILITY = "probability";

    /**
     * default value for state interval update
     */
    public static final double DEFAULT_STATE_UPDATE_INTERVAL = 300;
    /**
     * default value for boltzmann constant value
     */
    public static final double DEFAULT_BOLTZMANN_C = 0.1;
    /**
     * default value for congestion threshold
     */
    public static final double DEFAULT_CTH = 66;
    /**
     * default value for non-congestion threshold
     */
    public static final double DEFAULT_NCTH = 33;

    //default value for prbability
    public static final double DEFAULT_PROBABILITY = 0.333;

    /**
     * Queue mode for rate, reps, and TTL.
     */
    public static final int Q_MODE_RATE = 0;
    public static final int Q_MODE_REPS = 1;
    public static final int Q_MODE_RECIVETIME = 2;

    /**
     * check if the message has the oldest receiving time
     */
    //public boolean dropByOldestReceivingTime = true;
    public boolean dropByoldestTTL = true;

    /**
     * determine which queue mode used for dropping messages based on rate, reps
     * and ttl
     */
    public int deleteQueueMode;

    /**
     * value of stateUpdateInterval setting
     */
    private double stateUpdateInterval;
    /**
     * value of boltzmann setting
     */
    private double boltzmann;
    /**
     * value of CTH setting
     */
    private double CTH;
    /**
     * value of NCTH setting
     */
    private double NCTH;

    //value of probability
    private double probability;

    /**
     * dummy variable to count number of reps
     */
    private int nrofreps = 0;
    /**
     * dummy variable to count number of drops
     */
    private int nrofdrops = 0;

    // private double otherBuffer = 0;
    /**
     * to count msg limit for each connection
     */
    private int msglimit = 1;

    /**
     * ratio of drops and reps
     */
    /**
     * ratio bufferOcupanccy
     */
    private double bufferOcc = 0;

    /**
     * a map to record information about a connection and its limit
     */ //untk mencatat tentang koneksi dan batasan
    private Map<Connection, Integer> conlimitmap;

    /**
     * dummy variable to set the interval to count the new CV //untuk mengatur
     * intelval menghitung waktu to detect the new state
     */ //untuk mendeteksi state baru
    private double LastUpdateTimeofState = 0;

    /**
     * needed for CV report
     */ //ini bisa digati nnti degan menggunakan report buffer occpancy
    private List<BufferandTime> bufferandtime;

    /**
     * buffer that save receipt
     */ //mneyimpan tanda terima
    protected Map<String, ACKTTL> receiptBuffer;

    /**
     * message that should be deleted
     */ //pesan yang akan/harus di hapus
    protected Set<String> messageReadytoDelete;

    /**
     * QL object init
     */
    private QLearning QL;

    /**
     * action restriction checking table for each state
     */
    /**
     * tabel pengecekan pembatasan tindakan untuk setiap state
     */
    //ini nnti disesuaikan sama jumlah state yang dipake terus sama jumah action yang ada, sesuaikan sama tabel yang ada di proposal
    protected boolean[][] actionRestriction = {
        {true, true, true, true},
        {true, true, true, true},
        {false, false, false, false}};

    /**
     * exploration policy
     */ //ekplorasi kebijakan 
    protected IExplorationPolicy explorationPolicy;

    /**
     * init state for congested
     */
    private static final int C = 0;
    /**
     * init state for Partial Congested
     */
    private static final int PC = 1;
    /**
     * init state for Non-congested
     */
    private static final int NC = 2;

    /**
     * For the first time, the state value is set to 0 to tell the node that
     * this is the first time to do the learning. The node only need to observe
     * the current state and choose an action to receive the first q-value.
     */
    /**
     * Untuk pertama kalinya, nilai state diatur ke 0 untuk memberi tahu node
     * bahwa ini adalah pertama kalinya melakukan pembelajaran. Node hanya perlu
     * mengamati keadaan saat ini dan memilih tindakan untuk menerima nilai q
     * pertama.
     */
    protected int oldstate = -1;

    /**
     * to save the information about the last selected action(untuk menympan
     * informasi mengenai tindakan yang dipilih terakhir)
     */
    protected int actionChosen;

    /**
     * message generation interval in seconds (interval pembuatan pesan dalam
     * hitungan detik)
     */
    protected double msggenerationinterval = 600;

    /**
     * constant k for increase or decrease message generation period
     */
    //private double k = 2;
    /**
     * to record the last time of message creation (untuk mencatat waktu
     * terakhir pembuatan pesan)
     */
    private double endtimeofmsgcreation = 0;

    /**
     * message property to record its number of copies (properti pesan untuk
     * mencatat jumlah salinannya)
     */
    public static final String repsproperty = "nrofcopies";
    //conteinskey buat ngecek map itu punya dia apa enggak

    /**
     * Constructor. Creates a new message router based on the settings in the
     * given Settings object. (Membuat router pesan baru berdasarkan pengaturan
     * di objek Pengaturan yang diberikan.)
     *
     * @param s The settings object (untuk ngatur object)
     */
    public Epidemic_IQLCC_Cia1(Settings s) {
        super(s);
        Settings Epidemic_IQLCCSettings = new Settings(Epidemic_IQLCC_NS);

        if (Epidemic_IQLCCSettings.contains(STATE_UPDATE_INTERVAL_S)) {
            stateUpdateInterval = Epidemic_IQLCCSettings.getDouble(STATE_UPDATE_INTERVAL_S);
        } else {
            stateUpdateInterval = DEFAULT_STATE_UPDATE_INTERVAL;
        }

        if (Epidemic_IQLCCSettings.contains(BOLTZMANN_C_S)) {
            boltzmann = Epidemic_IQLCCSettings.getDouble(BOLTZMANN_C_S);
        } else {
            boltzmann = DEFAULT_BOLTZMANN_C;
        }

        if (Epidemic_IQLCCSettings.contains(CTH_S)) {
            CTH = Epidemic_IQLCCSettings.getDouble(CTH_S);
        } else {
            CTH = DEFAULT_CTH;
        }

        if (Epidemic_IQLCCSettings.contains(NCTH_S)) {
            NCTH = Epidemic_IQLCCSettings.getDouble(NCTH_S);
        } else {
            NCTH = DEFAULT_NCTH;
        }

        if (Epidemic_IQLCCSettings.contains(PROBABILITY)) {
            probability = Epidemic_IQLCCSettings.getDouble(PROBABILITY);
        } else {
            probability = DEFAULT_PROBABILITY;
        }

        explorationPolicy();
        initQL();
        limitconmap();
        buffertimelist();
        receiptbuffer();
        msgreadytodelete();
    }

    /**
     * Copyconstructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected Epidemic_IQLCC_Cia1(Epidemic_IQLCC_Cia1 r) {
        super(r);
        this.stateUpdateInterval = r.stateUpdateInterval;
        this.boltzmann = r.boltzmann;
        this.CTH = r.CTH;
        this.NCTH = r.NCTH;
        this.probability = r.probability;
        explorationPolicy();
        initQL();
        limitconmap();
        buffertimelist();
        receiptbuffer();
        msgreadytodelete();
    }

    /**
     * Initializes exploration policy
     */ //inisialisasi kebijakan explorasi
    protected void explorationPolicy() {
        this.explorationPolicy = new BoltzmannExploration(1);
    }

    protected void initQL() { //inisialisasi Qlearning

        this.QL = new QLearning(this.actionRestriction.length, this.actionRestriction[0].length, this.explorationPolicy,
                false, this.actionRestriction);

    }

    protected void limitconmap() {
        this.conlimitmap = new HashMap<Connection, Integer>();
    }

    protected void buffertimelist() {
        this.bufferandtime = new ArrayList<BufferandTime>();
    }

    protected void receiptbuffer() {
        this.receiptBuffer = new HashMap<>();
    }

    protected void msgreadytodelete() {
        this.messageReadytoDelete = new HashSet<>();
    }

    @Override
    public void changedConnection(Connection con) {
        if (con.isUp()) { //proses event

            connectionUp(con);
            /*peer's router */
            DTNHost otherHost = con.getOtherNode(getHost()); //membuat conennection dengan tetangganya
            conlimitmap.put(con, this.msglimit);  //menyimpann niai batas pesan untuk setiapp connection
            Collection<Message> thisMsgCollection = getMessageCollection(); //
            Epidemic_IQLCC_Cia1 peerRouter = (Epidemic_IQLCC_Cia1) otherHost.getRouter(); //deklarasi objek
            exchangemsginformation(); //bertukar informasi mengenai pesan yang sudah diterima atau belum, jika pesan itu sudah diterima maka alau ada pesan yang sama nda usah ditrima lagi
            Map<String, ACKTTL> peerRB = peerRouter.getReceiptBuffer(); //mengambil informasi mengenai tanda terima di buffer, hasilnya berupa Map yang berisi pasanan key-value antara ID Pesan dan ack dan ttl yang di simpan dalam variabel peerRB
            for (Map.Entry<String, ACKTTL> entry : peerRB.entrySet()) {
                if (!receiptBuffer.containsKey(entry.getKey())) { //mengecek apakah sudah pesan sudah pernah di terima atau belum
                    receiptBuffer.put(entry.getKey(), entry.getValue()); //kalau belum pesannya di terima
                }
            }
            for (Message m : thisMsgCollection) {
                /**
                 * Delete message that have a receipt (mengahpus pesan kalau
                 * puya salinannya)
                 */
                if (receiptBuffer.containsKey(m.getId())) {
                    messageReadytoDelete.add(m.getId());
                }
            }
            // delete transferred msg
            for (String m : messageReadytoDelete) {
                deletemsg(m, false);
            }
            // i think ini untuk mengurangi overhead rationya, agar tidak banyak pesan yang redudansi di jaringan.

            messageReadytoDelete.clear();
        } else { //pada connection down ini antar router saling bertukan informasi mengenai jumlah buffer yang tersisa pada router masing-masing
            connectionDown(con); //proses event
            conlimitmap.remove(con);
            //messageReadytoDelete.clear();
        }
    }

    /**
     * before deleting the message, check if the message is being sent
     */
    public void deletemsg(String msgID, boolean dropchecking) {//mengecek pesan, apakah pesan ini sementara di kirim atau nda
        if (isSending(msgID)) {
            List<Connection> conList = getConnections();
            for (Connection cons : conList) {
                if (cons.getMessage() != null && cons.getMessage().getId() == msgID) {
                    cons.abortTransfer();
                    break;
                }
            }
        }
        deleteMessage(msgID, dropchecking);
    }

    @Override
    public void update() {
        super.update();
        if ((SimClock.getTime() - LastUpdateTimeofState) >= stateUpdateInterval) {
            double newBufferOcc = getBufferOccupancyCurrent();
            newBufferOcc = (newBufferOcc <= 100.0) ? (newBufferOcc) : (100.0);
            if (this.oldstate == -1) {//buat learning pertama kali
                oldstate = staterequirement(this.bufferOcc, newBufferOcc);
                actionChosen = this.QL.GetAction(oldstate);
                this.actionSelectionController(actionChosen);
            } else {
                int newState = staterequirement(this.bufferOcc, newBufferOcc);
                this.updateState(newState);
            }
            this.bufferOcc = newBufferOcc;
            LastUpdateTimeofState = SimClock.getTime();
        }
        if (!canStartTransfer() || isTransferring()) {
            return; //ntng to transfer or currently transferring
        }
        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // then try any/all message to any/all connection
        // this.tryAllMessagesToAllConnections();
        /**
         * use it in the routing class
         */
        // tryAllMessageToAllConections();
    }

    /* exchange mesage's information of the reps number **///pertukaran informasi pesan mengenia jumlah replikasi pesan
    protected void exchangemsginformation() {
        Collection<Message> msgCollection = getMessageCollection();
        for (Connection con : getConnections()) {
            DTNHost peer = con.getOtherNode(getHost());
            Epidemic_IQLCC_Cia1 other = (Epidemic_IQLCC_Cia1) peer.getRouter();
            if (other.isTransferring()) {
                continue; // skip hosts that are transferring(melewati node kalau sedang tf)
            }
            for (Message m : msgCollection) {
                if (other.hasMessage(m.getId())) {
                    Message temp = other.getMessage(m.getId());
                    /* take the max reps */
                    if ((Integer) m.getProperty(repsproperty) < (Integer) temp.getProperty(repsproperty)) {
                        m.updateProperty(repsproperty, temp.getProperty(repsproperty));
                    }
                }

            }
        }
    }

    /* the procedure of updating the state*/ //untuk megupdate setiap statenya
    protected void updateState(int newstate) {
        double reward = checkReward(oldstate, newstate); //mengambil nilai dari mtdh chekreward dengan parameter oldstate dan newstate untuk peernya
        this.QL.UpdateState(oldstate, actionChosen, reward, newstate); //qlearning untuk update statenya
        int newestAction = this.QL.GetAction(newstate);
        this.actionSelectionController(newestAction); //memilih action
        this.oldstate = newstate;
        this.actionChosen = newestAction;
        System.out.println(newstate);
        System.out.println(reward);
        System.out.println(newestAction);
        System.out.println("");

        //coba nanti buat di sini statenya (ini buat ngecek aja sih)
//        if (newstate > CTH) { 
//            System.out.println("Jumlah bufferocc = " + newstate);
//            System.out.println("Congested");
//            System.out.println(newestAction);
//            System.out.println(reward);
//            System.out.println("");
//        } else if (newstate > NCTH) {
//            System.out.println("Jumlah bufferocc = " + newstate);
//            System.out.println("Partial C");
//            System.out.println(newestAction);
//            System.out.println(reward);
//            System.out.println("");
//        } else {
//            System.out.println("Jumlah bufferocc = " + newstate);
//            System.out.println("Non-Congested");
//            System.out.println(newestAction);
//            System.out.println(reward);
//            System.out.println("");
//        }
//         System.out.println(state);
//        System.out.println(newstate);
        BoltzmannExploration exp = (BoltzmannExploration) this.QL.getExplorationPolicy(); //menghitung qvalue
        double temp = exp.getTemperature();
        if (temp != 0) {
            /**
             * do exploration 100 times
             */
            exp.setTemperature(temp - boltzmann);
        }
        if (temp <= 0) {
            exp.setTemperature(0);
        }
        this.QL.setExplorationPolicy(exp);
        //}
    }

    /**
     * action selection controller
     */
    /**
     * public void actionSelectionController(int action) { //untuk memilih
     * action mana yang akan diambil pada state yang sedang dialami oleh buffer
     * switch (action) { case 0: this.dropbasedonhighestrate(); break; case 1:
     * this.dropbasedonhighestnrofreps(); break; case 2:
     * this.dropbasedonoldestTTL(); break; case 3:
     * this.dropbasedonoldestReceivingTime(); break; } } *
     */ //punya kak aca
    public void actionSelectionController(int action) { //untuk memilih action mana yang akan diambil pada state yang sedang dialami oleh buffer
        //  double state = this.getHost().getBufferOccupancy();
        switch (action) {
            case 0:
                this.dropbasedonhighestrepicationrate();
                break;
            case 1:
                this.dropbasedonhighestnrofreps();
                break;
            case 2:
                this.dropbasedonoldestReceivingTime();
                break;
            case 3:
                this.dropbasedonoldestTTL();
                break;
        }
    }

    /**
     * state transition's requirement
     */
    protected int staterequirement(double oldBufferocc, double newBufferocc) { //menentukan state dari buffer
        if (newBufferocc >= CTH) {
            return C;
        } else if (newBufferocc > NCTH && newBufferocc < CTH) {
            return PC;
        } else {
            return NC;
        }
    }

    @Override
    protected int startTransfer(Message m, Connection con) {
        int retVal;

        if (!con.isReadyForTransfer()) {
            return TRY_LATER_BUSY;
        }
        /* start transferring if the connection still has the remaining msg limit*///mulai mentransfer jika koneksi masih memiliki batas msg yang tersisa
        if (conlimitmap.containsKey(con)) {
            retVal = con.startTransfer(getHost(), m);
            if (retVal == RCV_OK) { // started transfer
                addToSendingConnections(con);
                /* mengatur batas yang tersisa dari koneksi sebagai batas yang tersisa */
                int remaininglimit = conlimitmap.get(con);
                //jika psannya di tf maka nilai batasannaya berkurag 1
                remaininglimit = remaininglimit - 1;
                /* if there's still any limit left, set the remaining limit as the new one. (jika masih ada batas yang tersisa, maka tetapkan batas tersebut sg batas yang baru)
				 * if there's no any limit left, remove the connection to prevent the node (jika tidak ada batas yang tersisa, maka hapus koneksi guna mencegah node trnsmt mengirim pesan ke koneksi tersebut
				 * from a sending a message to the connection.*/
 /* jika masih ada batas yang tersisa, tetapkan batas yang tersisa sebagai batas yang baru.
				 * jika tidak ada batas yang tersisa, hapus koneksi untuk mencegah node 
				 * dari mengirim pesan ke koneksi tersebut.*/
                if (remaininglimit != 0) {
                    conlimitmap.replace(con, remaininglimit);
                } else {
                    conlimitmap.remove(con);
                }
            } else if (deleteDelivered && retVal == DENIED_OLD && m.getTo() == con.getOtherNode(this.getHost())) {
                /* final recipient has already received the msg -> delete it */
                this.deleteMessage(m.getId(), false);
            }
            return retVal;
        }

        return DENIED_UNSPECIFIED;

    }

    /**
     * buffer checking
     */
    //jika state == C maka dia akan melakukan action drop ttl
    //else dia akan drop by quemode
    @Override
    protected boolean makeRoomForMessage(int size) { //nnti di perbaiki di sini ya cia untuk yang forced drop dan random drop nya
        if (size > this.getFreeBufferSize()) {
            return false; //ukuran pesan lebih besar dibandingkan dengan sisa buffer yang tersedia
        }
        int bufferFree = this.getFreeBufferSize();
        if (this.getHost().getBufferOccupancy() > CTH) {//forced drop
//            List<Message> messages = new ArrayList<>(this.getMessageCollection());
//            deleteSortByQueueMode(messages);
            if (dropByoldestTTL) {
                while (bufferFree < size) {
                    Message m = getOldestMessage(true);
                    if (m == null) {
                        return false; //tidak dapat mengahapus pesan apapun
                    }
                    deleteMessage(m.getId(), true);
                    nrofdrops++;
                    bufferFree += m.getSize();
                }
                return true;
            }
        } else {//buffer sudah masuk ke keadaan partial congested
            double randomValue = Math.random(); //untuk mengambil nlai yang digunakan untuk melakukan perbandingan dengan probability konstan yang sudah ada
            List<Message> message = new ArrayList<Message>(this.getMessageCollection()); //buat var msg yang mau d hapus
            if (randomValue <= probability) { //ini kalau lebih keci random valnya maka pesannya di hapus dengan random, based on RED
                // System.out.print("randmV" + randomValue);
                deleteSortByQueueMode(message); //ini metode random dropnya
                for (Message m : message) {
                    if (bufferFree < size) { //Kalau buffernya masih belum cukup
                        deleteMessage(m.getId(), true); //makan pesannya akan di hapus berdasarkan ID nya
                        nrofdrops++; //jumlahnya drop pesannya bertambah kalau ada pesan yang di hapus
                        bufferFree += m.getSize();
                    } else {
                        return true; //ini kalau buffernya
                    }
                    if (bufferFree < size) {
                        return true;
                    } else {
                        return false;
                    }
                }

            }
        }
        return true;
    }

    /**
     * to sort delete queue
     */
    @SuppressWarnings(value = "unchecked")
    /* ugly way to make this generic */
    protected List deleteSortByQueueMode(List list
    ) { //nnti ini di ubah urutannya yang qttl di ubah menjadi reciving time
        switch (deleteQueueMode) {
            /**
             * Compares messages by the highest rate
             */
            case Q_MODE_RATE:
                Collections.sort(list, new Comparator() {

                    public int compare(Object o1, Object o2) {
                        double diff;
                        Message m1, m2;

                        if (o1 instanceof Tuple) {
                            m1 = ((Tuple<Message, Connection>) o1).getKey();
                            m2 = ((Tuple<Message, Connection>) o2).getKey();
                        } else if (o1 instanceof Message) {
                            m1 = (Message) o1;
                            m2 = (Message) o2;
                        } else {
                            throw new SimError("Invalid type of objects in " + "the list");
                        }
                        double r1 = ((double) m1.getHops().size() - 1.0)
                                / ((double) m1.getInitTTL() - (double) m1.getTtl());
                        double r2 = ((double) m2.getHops().size() - 1.0)
                                / ((double) m2.getInitTTL() - (double) m2.getTtl());

                        /* descending sort */
                        if (r2 - r1 == 0) {
                            /* equal probabilities -> let queue mode decide */
                            return 0;
                        } else if (r2 - r1 < 0) {
                            return -1;
                        } else {
                            return 1;
                        }

                    }
                });
                break;
            case Q_MODE_REPS:
                Collections.sort(list, new Comparator() {
                    /**
                     * Compares messages by the highest number of replications
                     */
                    public int compare(Object o1, Object o2) {
                        double diff;
                        Message m1, m2;

                        if (o1 instanceof Tuple) {
                            m1 = ((Tuple<Message, Connection>) o1).getKey();
                            m2 = ((Tuple<Message, Connection>) o2).getKey();
                        } else if (o1 instanceof Message) {
                            m1 = (Message) o1;
                            m2 = (Message) o2;
                        } else {
                            throw new SimError("Invalid type of objects in " + "the list");
                        }
                        double reps1 = (Integer) m1.getProperty(repsproperty);
                        double reps2 = (Integer) m2.getProperty(repsproperty);

                        /* descending sort */
                        if (reps2 - reps1 == 0) {
                            /* equal probabilities -> let queue mode decide */
                            return 0;
                        } else if (reps2 - reps1 < 0) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                break;
            case Q_MODE_RECIVETIME://Q_MODE_RECIVETIME: //Nanti di ubah isinya
                Collections.sort(list, new Comparator() {
                    /**
                     * Compares messages by the oldest TTL
                     */
                    public int compare(Object o1, Object o2) {
                        double diff;
                        Message m1, m2;

                        if (o1 instanceof Tuple) {
                            m1 = ((Tuple<Message, Connection>) o1).getKey();
                            m2 = ((Tuple<Message, Connection>) o2).getKey();
                        } else if (o1 instanceof Message) {
                            m1 = (Message) o1;
                            m2 = (Message) o2;
                        } else {
                            throw new SimError("Invalid type of objects in " + "the list");
                        }
                        double ttl1 = m1.getTtl();
                        double ttl2 = m1.getTtl();


                        /* ascending sort */
                        if (ttl2 - ttl1 == 0) {
                            /* equal probabilities -> let queue mode decide */
                            return 0;
                        } else if (ttl2 - ttl1 < 0) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                });
                break;
            /* add more queue modes here */
            default:
                throw new SimError("Unknown queue mode " + deleteQueueMode);
        }
        return list;
    }

    @Override

    public boolean createNewMessage(Message m
    ) {
        if (this.endtimeofmsgcreation == 0
                || SimClock.getTime() - this.endtimeofmsgcreation >= this.msggenerationinterval) {
            this.endtimeofmsgcreation = SimClock.getTime();
            /* added repsproperty to count the 
			 * number of replications for a new message*/
            m.addProperty(repsproperty, 1);
            return super.createNewMessage(m);
        }
        return false;
    }

    @Override
    public Message messageTransferred(String id, DTNHost from
    ) {//recivier
        //ini buat mekanisme penerima pesan
        Message msg = super.messageTransferred(id, from);
        this.nrofreps++; // add -> this.nrOfReps++ jumlah replikasi pesan
        // - ACK -
        if (isFinalDest(msg, this.getHost()) && !receiptBuffer.containsKey(msg.getId())) {
            ACKTTL ack = new ACKTTL(SimClock.getTime(), msg.getTtl());
            receiptBuffer.put(msg.getId(), ack);
        }
        return msg;
//        Message msg = super.messageTransferred(id, from);
//        Integer msgprop = ((Integer) msg.getProperty(repsproperty)) + 1;
//        for (Connection con : getConnections()) {
//            DTNHost peer = con.getOtherNode(getHost());
//            Epidemic_IQLCC_Cia1 other = (Epidemic_IQLCC_Cia1) peer.getRouter();
//            if (isFinalDest(msg, this.getHost()) && !receiptBuffer.containsKey(msg.getId())) {
//                ACKTTL ack = new ACKTTL(SimClock.getTime(), msg.getTtl());
//                receiptBuffer.put(msg.getId(), ack);
//                if (getBufferOccupancyCurrent() > CTH) {
//                    makeRoomForMessage(msgprop);
//                }else{
//                    List<Message> message = new ArrayList<Message>(this.getMessageCollection());
//                    deleteSortByQueueMode(message);
//                }
//            }
//        }
//        msg.updateProperty(repsproperty, msgprop);
//        this.nrofreps++;
//        return msg;
    }

    /**
     * count message hops
     */
    protected int msgtotalhops() {
        Collection<Message> msg = getMessageCollection();
        int totalhops = 0;
        if (!msg.isEmpty()) {
            for (Message m : msg) {
                if (m.getHopCount() != 0) {
                    totalhops += (m.getHopCount() - 1);
                }
            }
        }
        return totalhops;
    }

    /**
     * check if this host is the final dest
     */
    protected boolean isFinalDest(Message m, DTNHost thisHost) {
        return m.getTo().equals(thisHost);
    }

    /**
     * IQL reward
     */
    protected double checkReward(int olds, int news) { //cara mencari nilai diantaran 0-maxvl, dan 0 - minvalue
        //nilai reward Max dan Min
        double max_value = 100;
        double min_value = -100;
        if (olds == NC && news == PC) {
            double reward = max_value / 2;
            return reward;
        } else if (olds == PC && news == PC) {
            return min_value;
        } else if (olds == PC && news == NC) {
            return 0;
        } else if (olds == PC && news == C) {
            return min_value;
        } else if (news == C) {
            return min_value;
        } else {
            return max_value;
        }
    }

    /**
     * IQL ACTION METHODS
     */
    // nnti yang force drop itu di ganti di make room dengan recive time
    private void dropbasedonhighestrepicationrate() {
        //  dropByOldestReceivingTime = false;
        dropByoldestTTL = false;
        deleteQueueMode = Q_MODE_RATE;
    }

    private void dropbasedonhighestnrofreps() {
        //dropByOldestReceivingTime = false;
        dropByoldestTTL = false;
        deleteQueueMode = Q_MODE_REPS;
    }

    private void dropbasedonoldestReceivingTime() {
        //  dropByOldestReceivingTime = false;
        dropByoldestTTL = false;
        deleteQueueMode = Q_MODE_RECIVETIME;
    }

    private void dropbasedonoldestTTL() {
        dropByoldestTTL = true;
    }

    /**
     * when connection up
     */
    public void connectionUp(Connection con) {

    }

    /**
     * when connection down
     */
    public void connectionDown(Connection con) {

    }

    public Map<String, ACKTTL> getReceiptBuffer() {
        return receiptBuffer;
    }

    public int getNrofReps() {
        return this.nrofreps;
    }

    public double getBufferOccupancyCurrent() {
        return this.getHost().getBufferOccupancy();
    }

    public int getNrofDrops() {
        return this.nrofdrops;
    }

    @Override
    public List<BufferandTime> getBufferandTime() {
        return this.bufferandtime;
    }

    @Override
    public double[][] getQV() {
        /* to record the q-values */
        return this.QL.getqvalues();
    }

}
