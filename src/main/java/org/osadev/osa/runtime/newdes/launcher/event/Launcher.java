/**+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--> 
<!--                Open Simulation Architecture (OSA)                  -->
<!--                                                                    -->
<!--      This software is distributed under the terms of the           -->
<!--           CECILL-C FREE SOFTWARE LICENSE AGREEMENT                 -->
<!--  (see http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html) -->
<!--                                                                    -->
<!--  Copyright © 2006-2015 Université Nice Sophia Antipolis            -->
<!--  Contact author: Olivier Dalle (olivier.dalle@unice.fr)            -->
<!--                                                                    -->
<!--  Parts of this software development were supported and hosted by   -->
<!--  INRIA from 2006 to 2015, in the context of the common research    -->
<!--  teams of INRIA and I3S, UMR CNRS 7172 (MASCOTTE, COATI, OASIS and -->
<!--  SCALE).                                                           -->
<!--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++**/ 
package org.osadev.osa.runtime.newdes.launcher.event;

import java.util.HashMap;

import org.objectweb.fractal.adl.Factory;
import org.objectweb.fractal.adl.FactoryFactory;
import org.objectweb.fractal.api.Component;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.util.Fractal;

import org.osadev.osa.simapis.simulation.SuperSchedulerControlItf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.osadev.osa.logger.basic.SimulationLogger;

public class Launcher {
	private static final Logger logger_ = LoggerFactory.getLogger(Launcher.class);

	//private static final SimulationLogger logger_ = new SimulationLogger(Launcher.class);
	
  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
      System.out.println("Usage: TODO");
      System.exit(0);
    }

    if (System.getProperty("fractal.provider") == null) {
    	logger_.error("[Error] Property missing: factal.provider");    	
    }
   
    System.setProperty("fractal.disableAllChecks", "true");

    final HashMap<Object, Object> ctx = new HashMap<Object, Object>();
    
    String simFactory = System.getProperty("adl.factory", 
    		"org.osadev.osa.engines.newdes.adl.SimBasicFactory");
  
    String fractalBackend = System.getProperty("fractal.backend", 
    		"org.osadev.osa.engines.newdes.adl.SimFractalBackend");
   
    
    Factory adlFactory =
      FactoryFactory.getFactory(simFactory, fractalBackend, ctx);

    logger_.trace("#################Launch#####################");
    Component appli = (Component) adlFactory.newComponent(args[0], ctx);

    

    logger_.trace("#################InitSimu#####################");
    // We need to retrieve the SuperScheduler from the root component
    Component[] topLevelComps = Fractal.getContentController(appli).getFcSubComponents();
    Component superSchedulerComp = null;
    SuperSchedulerControlItf superSchedItf = null;
    
    for (Component comp: topLevelComps) {
    	try {
    		logger_.debug("Launch: is {} the SS?", comp);
    		superSchedItf =(SuperSchedulerControlItf) comp.getFcInterface("superschedulercontrol");
    	} catch (NoSuchInterfaceException e) {
    		continue;
    	}
    	// No exception means we've found the SS
    	superSchedulerComp = comp;
    	break;
    }
    
    if (superSchedulerComp == null) {
    	throw new RuntimeException("Launcher: failed to retrieve SharedSS!");
    }
    
    logger_.debug("Launch: SS Found ({})",superSchedulerComp);
    // Need to call LCC to unlock SS server itf  
    Fractal.getLifeCycleController(superSchedulerComp).startFc();
    //SharedSuperSchedulerItf superSchedItf = (SharedSuperSchedulerItf)sharedSuperSchedulerComp.getFcInterface("superschedulersvc");
    // Deploy SS
    superSchedItf.deployAndBind(superSchedulerComp, appli);
    
    
    
    logger_.trace("#################StartFractal#####################");
    //((SimulationControllerAPI)appli).init();
    Fractal.getLifeCycleController(appli).startFc();
    
    
    logger_.trace("#################Run#####################");
    superSchedItf.startSimulation();
    
    logger_.trace("########### Run Complete (time={}) #############");
  }

}