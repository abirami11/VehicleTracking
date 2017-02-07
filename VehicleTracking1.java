import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class VehicleTracking1 {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	static Mat imag = null;
	static int secondFrame = 0;
	static boolean setStopFlag = false;
	static ArrayList<Rect> rectArray = new ArrayList<Rect>();
	static ArrayList<Mat> imageROIArray = new ArrayList<Mat>();
	static ArrayList<String> entityLabelArray = new ArrayList<String>();


	public static void main(String[] args) {
		JFrame jframe = new JFrame("Vehicle MOTION DETECTOR ");
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel videopanel = new JLabel();
		jframe.setContentPane(videopanel);
		jframe.setVisible(true);
		jframe.setSize(640, 480);

		Mat frame = new Mat();
		Mat outerFrame = new Mat();
		Mat differenceframe = null;
		Mat temporaryframe = null;
		ArrayList<Rect> contourRectangles = new ArrayList<Rect>();
		VideoCapture camera = new VideoCapture("/Users/Arwen/Documents/Capstone/Images/rheinhafen.mpg");
		//VideoCapture camera = new VideoCapture("/Users/Arwen/Documents/Capstone/Images/sherbrooke_video-quicktime.mov");
		//VideoCapture camera = new VideoCapture("/Users/Arwen/Documents/Capstone/Images/mv2_001-quicktime.mov");


		Size sz = new Size(640, 480);
		int i = 0, it = 0, count = 0;
		System.out.println(System.currentTimeMillis());
		while (it <=50) {
			//System.out.println("Frame Number = " + it);
			if (camera.read(frame)) {
				 Imgproc.resize(frame, frame, sz);
				imag = frame.clone();
				outerFrame = new Mat(frame.size(), CvType.CV_8UC1);
				Imgproc.cvtColor(frame, outerFrame, Imgproc.COLOR_BGR2GRAY);
				Imgproc.GaussianBlur(outerFrame, outerFrame, new Size(3, 3), 0);

				if (i == 0) {
					jframe.setSize(frame.width(), frame.height());
					differenceframe = new Mat(outerFrame.size(), CvType.CV_8UC1);
					temporaryframe = new Mat(outerFrame.size(), CvType.CV_8UC1);
					differenceframe = outerFrame.clone();
				}

				if (i == 1) {
					Core.subtract(outerFrame, temporaryframe, differenceframe);
					Imgproc.adaptiveThreshold(differenceframe, differenceframe, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
							Imgproc.THRESH_BINARY_INV, 5, 2);
					contourRectangles = contour_detection(differenceframe);
					if (contourRectangles.size() > 0) {
						drawRectangles(contourRectangles);
					}
				}

				i = 1;
				secondFrame++;

				ImageIcon image = new ImageIcon(Mat2bufferedImage(imag));
				videopanel.setIcon(image);
				videopanel.repaint();
				temporaryframe = outerFrame.clone();

			}
			it++;
		}
		//System.out.println(count);
		System.out.println(System.currentTimeMillis());

	}
	@SuppressWarnings("unchecked")
	private static void drawRectangles(ArrayList<Rect> contourRectangles) {
		HashSet<Rect> oldrects = new HashSet<Rect>();
		HashSet<Rect> newrects = new HashSet<Rect>();
		Rect rect = null;	
		Mat imageROI = null;

		Rect rectCrop = null;
		int iteration = 0;
		do {
			oldrects.clear();
			newrects.clear();
			Iterator<Rect> it2 = contourRectangles.iterator(); 
			for (int x = 0; x < contourRectangles.size(); x++) {
				for (int y = x + 1; y < contourRectangles.size(); y++) {
					Rect rect1 = contourRectangles.get(x);
					Rect rect2 = contourRectangles.get(y);
					if (doesItIntersect(rect1, rect2)) {
						Rect newrect = merge(rect1, rect2);
						newrects.add(newrect);
						oldrects.add(rect1);
						oldrects.add(rect2);
					} 
				}
			}
			contourRectangles.removeAll(oldrects);
			contourRectangles.addAll(newrects);
			iteration++;
		} while (oldrects.size() > 0);
		Iterator<Rect> it5 = contourRectangles.iterator();
		HashSet<Rect> humanrects = new HashSet<Rect>();

		while (it5.hasNext()) {
			Rect rectHuman = it5.next();
			if(rectHuman.width <= 40) {
				humanrects.add(rectHuman);
			}
		}
		contourRectangles.removeAll(humanrects);
		Iterator<Rect> it3 = contourRectangles.iterator();
		
		//System.out.println(contourRectangles.size());
		//System.out.println("Frame Number = " +  secondFrame);
		if(secondFrame == 1 || secondFrame%5 == 0) {
			//rect = contourRectangles.get(4);
			rectArray.clear();
			imageROIArray.clear();
			entityLabelArray.clear();
			while (it3.hasNext()) {
				rect = it3.next();
				rectArray.add(rect);
				imageROI = new Mat(imag,rect);
				imageROIArray.add(imageROI);
				//Imgcodecs.imwrite("/Users/Arwen/Documents/Capstone/Images/veh1.jpg",imageROI);
				//System.out.println("Br = " + rect.br() + " and Tl = " +rect.tl());
				String entityLabel = "";
				if(rect.area() > 6000 && !InBottomRightSide(rect.x, rect.y)) {
					entityLabel = "NON CAR";
					//System.out.println(rect.area());
					//System.out.println(rect.x + " " + rect.y);
					
				} else if(rect.area() > 20000 && InBottomRightSide(rect.x, rect.y)) {
					entityLabel = "NON CAR";

				}
				else {
					entityLabel = "CAR";
					
				}
				entityLabelArray.add(entityLabel);
				//System.out.print(entityLabel);
				Imgproc.putText(imag, entityLabel, new Point(rect.x -5,rect.y-5),
		                    Core.FONT_HERSHEY_PLAIN, 1.0 ,new  Scalar(255,0,0), 2);
				
				Imgproc.rectangle(imag, rect.br(), rect.tl(), new Scalar(0, 0, 255), 2);
			}
	

		}
		Iterator<Rect> it4 = contourRectangles.iterator();
		if(secondFrame >= 2 && (secondFrame%5 != 0)) {
			for(int rcount = 0; rcount < rectArray.size(); rcount++) {			
				trackObject(rectArray.get(rcount), imageROIArray.get(rcount), entityLabelArray.get(rcount));
			//trackObject(rectArray.get(1), imageROIArray.get(1), entityLabelArray.get(1));
			}
			//System.out.println();
		}
		

				
	}
	private static boolean InBottomRightSide(int x, int y) {
		if(y > 240)
			return true;
		return false;
	}
	private static void trackObject(Rect rect, Mat imageROI, String entityLabel) {
		Mat image_roi = null;
		double minDist = 100000000, distance = 0;
		int nearestI = 0;
		boolean setFoundCarFlag = false;
		//int r = 0;
		HashMap<Integer, Double> savedDistances = new HashMap<Integer, Double>();

		for(int k = 0; k < 25; k++) {
			//String vehicleName = "vehi" + r;
			//Imgcodecs.imwrite("/Users/Arwen/Documents/Capstone/Images/" + vehicleName + ".jpg",imageROI);
			//r++;
			List<Mat> imageSegments = new ArrayList<Mat>();
			HashMap<Mat,HashMap<Integer,Integer>> imageHashMap = new HashMap<Mat,HashMap<Integer,Integer>>();
			//This will not work for values that are in the boundaries of the frame
			for( int i = -1; i < 2; i++) {
				for(int j = -1; j < 2; j++) {
					Mat imageMatrix = new Mat(imag, new Rect(rect.x+i, rect.y+j, rect.width, rect.height));
					imageSegments.add(imageMatrix);
					imageHashMap.put(imageMatrix,
							new HashMap<Integer, Integer>());
					imageHashMap.get(imageMatrix).put(i, j);

				}
			}
			HashMap<Integer, Double> edistances = new HashMap<Integer, Double>();
			for(int i = 0; i < 9; i++){ 
				if(savedDistances.containsKey(i)) {
					distance = savedDistances.get(i);
					//System.out.println("Im in");
				} else {
					distance = euclideanDistancebetweenTwoMatrices.findLeastEuclideanDistance(imageROI, imageSegments.get(i));
				}
				edistances.put(i, distance);
				if(minDist > distance) {
					minDist = distance;
					nearestI = i;
				}
			}
			//System.out.println(minDist + " is in " + nearestI + "th position and k value is " + k);
			if(nearestI == 4 ) {
				if(minDist < 49000000.00) {
				//System.out.println("done");
					setFoundCarFlag = true;
				} else {
					setFoundCarFlag = false;
					setStopFlag = true;
				}
				break;
			}
				
			else {
				savedDistances.clear();
				savedDistances = reduceBoxesInTracking.useImageSegments(imageSegments, nearestI, edistances);
				image_roi = imageSegments.get(nearestI);
				HashMap<Integer, Integer> resultMap = new HashMap<Integer, Integer>();
				resultMap = imageHashMap.get(image_roi);
				Set<Integer> keySet = resultMap.keySet();
				int value = 0, key = 0;
				for(int n: keySet) {
					value = resultMap.get(n);
					key = n;
					setFoundCarFlag = false;
				}
				rect = new CornersCorrection().cornerCorrection(rect, key, value);
				minDist = 100000000;
			}
		}
		//System.out.print(entityLabel);

		Imgproc.putText(imag, entityLabel, new Point(rect.x -5,rect.y-5),
                Core.FONT_HERSHEY_PLAIN, 1.0 ,new  Scalar(255,0,0), 2);
		Imgproc.rectangle(imag, rect.br(), rect.tl(), new Scalar(0, 0, 255), 2);

		//if(setFoundCarFlag == true && setStopFlag == false) {
		//}
	}
	public static BufferedImage Mat2bufferedImage(Mat image) {
		MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".jpg", image, bytemat);
		byte[] bytes = bytemat.toArray();
		InputStream in = new ByteArrayInputStream(bytes);
		BufferedImage img = null;
		try {
			img = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
	}

	public static ArrayList<Rect> contour_detection(Mat out) {
		Mat hierarchy = new Mat();
		Mat vv = out.clone();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(vv, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		// System.out.println(hierarchy);
		 
		double maxArea = 100;
		int maxAreaIdx = -1;
		Rect r = null;
		ArrayList<Rect> rect_array = new ArrayList<Rect>();
		for (int idx = 0; idx < contours.size(); idx++) {
			Mat contour = contours.get(idx);
			double contourarea = Imgproc.contourArea(contour);
			if (contourarea > maxArea) {
				// maxArea = contourarea;
				maxAreaIdx = idx;
				r = Imgproc.boundingRect(contours.get(maxAreaIdx));
				rect_array.add(r);
				//Imgproc.drawContours(imag, contours, maxAreaIdx, new
				// Scalar(0,0, 255));
			}

		}

		hierarchy.release();

		return rect_array;

	}
	private static boolean doesItIntersect(Rect A, Rect B) {
		/*
		 * int left = max(A.x, B.x); int top = max(A.y, B.y); int right =
		 * min(A.x + A.width, B.x + B.width); int bottom = min(A.y + A.height,
		 * B.y + B.height); if(left <= right && top <= bottom) return new
		 * Rect(left, top, right - left, bottom - top); else return new Rect();
		 */
		Rectangle r1 = new Rectangle(A.x, A.y, A.width, A.height);
		Rectangle r2 = new Rectangle(B.x, B.y, B.width, B.height);
		return (r1.intersects(r2));
		
	}

	private static int max(int a, int b) {
		if (a >= b)
			return a;
		else
			return b;
	}

	private static int min(int a, int b) {
		if (a <= b)
			return a;
		else
			return b;
	}
	private static Rect merge(Rect A, Rect B) {
		int left = min(A.x, B.x);
		int bottom = min(A.y, B.y);
		int right = max(A.x + A.width, B.x + B.width);
		int top = max(A.y + A.height, B.y + B.height);
		return new Rect(left, bottom, right - left, top - bottom);
	}
}
