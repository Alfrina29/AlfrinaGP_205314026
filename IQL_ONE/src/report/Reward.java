/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.SimScenario;
import java.util.*;
import core.DTNHost;



/**
 *
 * @author ASPIRE 5
 */
public class Reward extends Report {

    public Reward() {
        init();
    }
    
    public void done(){
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        
    }
}
