package edu.mines.acmX.exhibit.input_services.hardware.drivers;

import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.OpenNI.ActiveHandEventArgs;
import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMap;
import org.OpenNI.DepthMetaData;
import org.OpenNI.GeneralException;
import org.OpenNI.GestureGenerator;
import org.OpenNI.GestureRecognizedEventArgs;
import org.OpenNI.HandsGenerator;
import org.OpenNI.IObservable;
import org.OpenNI.IObserver;
import org.OpenNI.ImageGenerator;
import org.OpenNI.ImageMetaData;
import org.OpenNI.InactiveHandEventArgs;
import org.OpenNI.Point3D;
import org.OpenNI.StatusException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.mines.acmX.exhibit.input_services.events.EventManager;
import edu.mines.acmX.exhibit.input_services.events.EventType;
import edu.mines.acmX.exhibit.input_services.events.InputReceiver;
import edu.mines.acmX.exhibit.input_services.hardware.HandPosition;
import edu.mines.acmX.exhibit.input_services.hardware.devicedata.DepthImageInterface;
import edu.mines.acmX.exhibit.input_services.hardware.devicedata.HandTrackerInterface;
import edu.mines.acmX.exhibit.input_services.hardware.devicedata.RGBImageInterface;
import edu.mines.acmX.exhibit.input_services.openni.OpenNIContextSingleton;
import edu.mines.acmX.exhibit.stdlib.input_processing.imaging.HandTrackingUtilities;

/**
 * Kinect driver that provides depth and rgb image functionality.
 * Uses the openni the library for communication to the kinect device.
 * 
 * @author Aakash Shah
 * @author Ryan Stauffer
 *
 */
public class KinectOpenNIDriver 
	implements 	DriverInterface, DepthImageInterface, RGBImageInterface,
				HandTrackerInterface {
	
	private static Logger log = LogManager.getLogger(KinectOpenNIDriver.class);
	public static final int HISTORY_SIZE = 10;
	public static final EventManager evtMgr = EventManager.getInstance();
	
	private Context context;
	
	private DepthGenerator depthGen;
	private ImageGenerator imageGen;
	private GestureGenerator gestureGen;
	private HandsGenerator handsGen;
	
	private Map<Integer, List<Point3D>> history;
	
	private int imageWidth;
	private int imageHeight;
	
	private int depthWidth;
	private int depthHeight;
	
	public KinectOpenNIDriver(){
		log.info("In the driver constructor");
         try {
        	context = OpenNIContextSingleton.getContext();
        	
        	gestureGen = GestureGenerator.create(context);
        	gestureGen.addGesture("Wave");
        	gestureGen.getGestureRecognizedEvent().addObserver(new GestureRecognized());
        	
        	handsGen = HandsGenerator.create(context);
        	handsGen.getHandCreateEvent().addObserver(new HandCreated());
        	handsGen.getHandUpdateEvent().addObserver(new HandUpdated());
        	handsGen.getHandDestroyEvent().addObserver(new HandDestroyed());
        	        	
			depthGen = DepthGenerator.create(context);
			DepthMetaData depthMD = depthGen.getMetaData();
			
			context.startGeneratingAll();
			
			history = new HashMap<Integer, List<Point3D>>();
			
			imageGen = ImageGenerator.create(context);
			
			ImageMetaData imageMD = imageGen.getMetaData();
			imageWidth = imageMD.getFullXRes();
			imageHeight = imageMD.getFullYRes();
			
			depthWidth = depthMD.getFullXRes();
			depthHeight = depthMD.getFullYRes();
			
		} catch (GeneralException e) {
			e.printStackTrace();
		}
	}
	
	public void updateDriver() {
		try {
			context.waitAnyUpdateAll();
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}
	
	class GestureRecognized implements IObserver<GestureRecognizedEventArgs>
	{

		@Override
		public void update(IObservable<GestureRecognizedEventArgs> observable,
				GestureRecognizedEventArgs args)
		{
			
			try {
				log.info("Found a wave");
				handsGen.StartTracking(args.getEndPosition());
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
	}
	
	class HandCreated implements IObserver<ActiveHandEventArgs> {

		@Override
		public void update(IObservable<ActiveHandEventArgs> obs,
				ActiveHandEventArgs e) {
			evtMgr.fireEvent(EventType.HAND_CREATED, e.getId());
			List<Point3D> newList = new ArrayList<Point3D>();
			newList.add(e.getPosition());
			history.put(e.getId(), newList);
		}
		
	}
	
	class HandUpdated implements IObserver<ActiveHandEventArgs> {

		@Override
		public void update(IObservable<ActiveHandEventArgs> obs,
				ActiveHandEventArgs e) {
			List<Point3D> historyList = history.get(e.getId());
			historyList.add(e.getPosition());
			
			HandPosition pos = new HandPosition(e.getId(),
					HandTrackingUtilities.convertOpenNIPoint(depthGen, e.getPosition()));
			
			evtMgr.fireEvent(EventType.HAND_UPDATED, pos);
			
			if (historyList.size() > HISTORY_SIZE) {
				historyList.remove(0);
			}
		}
	}
	
	class HandDestroyed implements IObserver<InactiveHandEventArgs> {

		@Override
		public void update(IObservable<InactiveHandEventArgs> obs,
				InactiveHandEventArgs e) {
			evtMgr.fireEvent(EventType.HAND_DESTROYED, e.getId());
			history.remove(e.getId());
			
		}
		
	}
	
	
	// DriverInterface
	public boolean isAvailable() {
		boolean ret = true;
		try {
			context = OpenNIContextSingleton.getContext();
		} catch (GeneralException e) {
			ret = false;
		}
		return ret;
	}
	
	// DepthDataInterface
	public ShortBuffer getDepthImageData() {
		DepthMetaData depthMD = depthGen.getMetaData();
		
		DepthMap dm = depthMD.getData();
		ShortBuffer data = dm.createShortBuffer();
		data.rewind();
		return data;
	}
	
	public int getRGBImageWidth() {
		return imageWidth;
	}
	
	public int getRGBImageHeight() {
		return imageHeight;
	}
	
	// RGBImageInterface
	public ByteBuffer getVisualData() {
		ImageMetaData imageMD = imageGen.getMetaData();
		ByteBuffer rgbBuffer = imageMD.getData().createByteBuffer();
		return rgbBuffer;
	}
	
	public int getDepthImageWidth() {
		return depthWidth;
	}
	
	public int getDepthImageHeight() {
		return depthHeight;
	}

	@Override
	public void registerGestureRecognized(InputReceiver r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerHandCreated(InputReceiver r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerHandUpdated(InputReceiver r) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerHandDestoryed(InputReceiver r) {
		// TODO Auto-generated method stub
		
	}

}
