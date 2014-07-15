/*******************************************************************************
 * Copyright (c) 2014 Andrei Stoica. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the GNU Public License v3.0 which accompanies
 * this distribution, and is available at http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors: Andrei Stoica - initial API and implementation, Year: 2014
 ******************************************************************************/

package edu.tum.cs.vis.model.util.algorithm;

import java.lang.Math;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.vecmath.Vector3f;

import edu.tum.cs.vis.model.Model;
import edu.tum.cs.vis.model.util.Cluster;
import edu.tum.cs.vis.model.util.Curvature;
import edu.tum.cs.vis.model.util.Edge;
import edu.tum.cs.vis.model.util.Region;
import edu.tum.cs.vis.model.util.Triangle;
import edu.tum.cs.vis.model.util.Vertex;
import edu.tum.cs.ias.knowrob.utils.ThreadPool;


/**
 * Class that implements the processing blocks used to
 * render better segmentation results of the models 
 * analyzed. Blocks can be either pre- or post-processing
 * units in the computation flow applied to the model
 * 
 * @author Andrei Stoica
 */
public class ModelProcessing{

	private static Logger		logger = Logger.getLogger(ModelProcessing.class);
	/**
	 * Model to be processed
	 */
	protected Model				model;
	
//	/**
//	 * Flag that shows if sharp edge detection has been done
//	 */
//	private boolean 			modelSharpEdgeDetectionCheck = false;
	
	/**
	 * Flag that shows if the classification of the curvatures has been done
	 */
	private boolean				modelKMeansCurvatureClassification = false;
	
	/**
	 * Defines the number of clusters used for classifying the vertices
	 */
	private static int			NUM_OF_CLUSTERS = 5;
	
	/**
	 * Defines upper iteration limit for the KMeans algorithm
	 */
	private static int			UPPER_ITERATION_LIMIT = 100;
	
//	/**
//	 * Number of added triangles to the model
//	 */
//	private int 				numAddedTriangles = 0;
	
	/**
	 * Default constructor of the ModelProcessing class
	 */
	public ModelProcessing() {
		this.model = null;
	}
	
	/**
	 * Constructor of the ModelProcessing class
	 * 
	 * @param newModel 
	 * 			model to be analyzed
	 */
	public ModelProcessing(Model newModel) {
		this.model = newModel;
	}
	
	/**
	 * Setter for the model of an instance
	 */
	public void setModel(Model newModel) {
		this.model = newModel;
	}
	
	/**
	 * Getter for the model of an instance
	 */
	public Model getModel() {
		return model;
	}
	
//	/**
//	 * Getter for the number of the added triangles
//	 */
//	public int getNumAddedTriangles() {
//		return numAddedTriangles;
//	}
	
	/**
	 * Gets number of clusters defined
	 */
	public int getNumOfClusters() {
		return NUM_OF_CLUSTERS;
	}
	
//	/**
//	 * Getter for the sharp edge detection flag
//	 */
//	public boolean isSharpEdgeDetectionChecked() {
//		return modelSharpEdgeDetectionCheck;
//	}
	
	/**
	 * Getter for the K Means classification of curvatures
	 */
	public boolean isKMeansCurvatureClassified() {
		return modelKMeansCurvatureClassification;
	}
	
//	/**
//	 * Detects the model sharp edges, marks them and adds additional 
//	 * points for the "sharp triangles" in order to correct
//	 * for problematic curvature computation
//	 */
//	public void sharpEdgeDetection() {
//		// remove any wrong tesselated triangles in the model
//		this.removeCollinearTriangles();
//		
//		this.numAddedTriangles = this.model.getTriangles().size();
//		
//		// perform the sharp edge detection for individual triangles (multi-threaded)
//		List<Callable<Void>> threads = new LinkedList<Callable<Void>>();
//
//		final int interval = 500;
//
//		for (int start = 0; start < model.getTriangles().size(); start += interval) {
//			final int st = start;
//			threads.add(new Callable<Void>() {
//
//				@Override
//				public Void call() throws Exception {
//					int end = Math.min(st + interval, model.getTriangles().size());
//					for (int i = st; i < end; i++) {
//						sharpEdgeDetectionForTriangle(model.getTriangles().get(i));
//					}
//					return null;
//				}
//
//			});
//		}
//		ThreadPool.executeInPool(threads);
//
////		for (int i = 0 ; i < model.getTriangles().size() ; ++i) {
////			Triangle t = model.getTriangles().get(i);
////			System.out.println(i + " " + t.getSharpEdges());
////		}
//		
//		// for sharp triangles add new point and introduce 3 new triangles
//		List<Triangle> toRemove = new ArrayList<Triangle>();
//		for (int i = 0 ; i < model.getTriangles().size() ; ++i) {
//			Triangle t = model.getTriangles().get(i);
//			if (t.checkIsSharpTriangle()) {
//				addTrianglesToModel(t);
//				toRemove.add(t);
//			}
//		}
//		model.getTriangles().removeAll(toRemove);
//		
////		this.removeCollinearTriangles();
//	
//		this.numAddedTriangles = this.model.getTriangles().size() - this.numAddedTriangles;
//		if (this.numAddedTriangles != 0) {
//			logger.debug("Added " + this.numAddedTriangles + " triangles to the model");
//		}
//		else {
//			logger.debug("No triangles added to the model");
//		}
//		//
//		this.modelSharpEdgeDetectionCheck = true;
//		
//		for (int i = 0 ; i < model.getTriangles().size() ; ++i) {
//			Triangle t = model.getTriangles().get(i);
//			System.out.println(i + " " + model.getTriangles().get(i) + "sharp edges:\n" + t.getSharpEdges() + "\n" + t.getNeighbors() + "\n");
//		}
//	}
//	
//	/**
//	 * Performs the sharp detection at the triangle level
//	 */
//	private void sharpEdgeDetectionForTriangle(Triangle t) {
//		synchronized (t) {
//		Iterator<Triangle> it = t.getNeighbors().iterator();
//		while (it.hasNext()) {
//			Triangle n = it.next();
//			synchronized (n) {
//			float angleOfNormals = (float)Math.toDegrees(t.getNormalVector().angle(n.getNormalVector()));
//			if ((angleOfNormals >= 80.0) && (angleOfNormals <= 110.0)) {
//				List<Vertex> vShared = findSharedVertices(t,n);
//				Edge edge = new Edge(vShared.get(0), vShared.get(2));
//				vShared.get(0).isSharpVertex(true);
//				vShared.get(1).isSharpVertex(true);
//				vShared.get(2).isSharpVertex(true);
//				vShared.get(3).isSharpVertex(true);
//				t.addSharpEdge(edge);
//				n.addSharpEdge(edge);
//			}
//			}
//		}
//		if (t.getNeighbors().size() < 3) {
//			Edge[] edges = t.getEdges();
//			for (int i = 0 ; i < edges.length ; ++i) {
//				Triangle n = t.getNeighborOfEdge(edges[i]);
//				if (n == null) {
//					// mark vertices that define the edge as being sharp
//					t.getPosition()[(i+1) % edges.length].isSharpVertex(true);
//					t.getPosition()[(i+2) % edges.length].isSharpVertex(true);
//					t.addSharpEdge(edges[i]);
//				}
//				
//			}
//		}
//		}
//	}
//	
//	/**
//	 * Finds the two common points of two neighboring trinagles
//	 */
//	private List<Vertex> findSharedVertices(Triangle t, Triangle n) {
//		List<Vertex> v = new ArrayList<Vertex>(4);
//		for (int i = 0 ; i < t.getPosition().length ; ++i) {
//			for (int j = 0 ; j < n.getPosition().length ; ++j) {
//				if (t.getPosition()[i].sameCoordinates(n.getPosition()[j])) {
//					v.add(t.getPosition()[i]);
//					v.add(n.getPosition()[j]);
//					break;
//				}
//			}
//		}
//		return v;
//	}
//	
//	/**
//	 * Adds triangles to model using the centroid of the triangle
//	 * 
//	 * @param t 
//	 * 			triangle decomposed in 3 smaller triangles
//	 */
//	private void addTrianglesToModel(Triangle t) {
//		Vertex newVertex = new Vertex(t.getCentroid().x, t.getCentroid().y, t.getCentroid().z);
//		model.getVertices().add(newVertex);
//		Triangle[] newTriangle = new Triangle[3];
//		for (int i = 0 ; i < 3 ; ++i) {
//			newTriangle[i] = new Triangle(t.getPosition()[i],t.getPosition()[(i+1)%3],newVertex);
//			newTriangle[i].setAppearance(t.getAppearance());
//			newTriangle[i].setNormalVector(t.getNormalVector());
//		}
//
//		// add neighbors inside the big triangle
//		newTriangle[0].addNeighbor(newTriangle[1]);
//		newTriangle[0].addNeighbor(newTriangle[2]);
//		newTriangle[1].addNeighbor(newTriangle[2]);
//		
//		// add triangle neighbors outside the original triangle 
//		Edge[] edges = t.getEdges();
//		for (int i = 0 ; i < edges.length ; ++i) {
//			Triangle n = t.getNeighborOfEdge(edges[i]);
//			if (n == null) {
//				continue;
//			}
//			for (int j = 0 ; j < 3 ; ++j) {
//				if (newTriangle[j].containsEdge(edges[i])) {
//					n.removeNeighbor(t);
//					n.addNeighbor(newTriangle[j]);
//					newTriangle[j].addNeighbor(n);
//					break;
//				}
//			}
//		}
//		
////		
////		List<Triangle> neighbors = new ArrayList<Triangle>();
////		neighbors.addAll(t.getNeighbors());
////		for (int i = 0 ; i < 3 ; ++i) {
////			for (int j = 0 ; j < neighbors.size() ; ++j) {
////				logger.debug("j = " + j);
////				Triangle n = neighbors.get(j);
////				int cont = 0;
////				for (Vertex v : n.getPosition()) {
////					if ((v.sameCoordinates(newTriangle[i].getPosition()[0])) || (v.sameCoordinates(newTriangle[i].getPosition()[1]))) {
////						cont++;
////					}
////				}
////				// if the neighboring triangle (exact 2 common vertices)
////				if (cont == 2) {
////					t.removeNeighbor(n);
////					neighbors.remove(n);
////					newTriangle[i].addNeighbor(n);
////					n.addNeighbor(newTriangle[i]);
////					break;
////				}
////			}
////		}
//		
//		// add sharp edges if any to the new 3 created triangles
//		for (Edge sharpEdge : t.getSharpEdges()) {
//			for (int i = 0 ; i < 3 ; ++i) {
//				newTriangle[i].addSharpEdge(sharpEdge);
//			}
//		}
//		
//		// add new vertex as direct neighbor and old vertices as neighbors for new one
//		// compute centroids of new triangles and add them to the model
//		for (int i = 0 ; i < 3 ; ++i) {
//			t.getPosition()[i].addNeighbor(newVertex);
//			newVertex.addNeighbor(t.getPosition()[i]);
//			newTriangle[i].updateCentroid();
//			model.getTriangles().add(newTriangle[i]);
//		}
//	}
//	
//	/**
//	 * Removes "colinear triangles", i.e. "triangles" which have 3 "colinear" vertices
//	 */
//	private void removeCollinearTriangles() {
//		List<Triangle> allTriangles = new ArrayList<Triangle>();
//		allTriangles.addAll(model.getTriangles());
//		int rmTriangles = 0;
//		for (int i = 0 ; i < allTriangles.size() ; ++i) {
//			Edge[] edges = allTriangles.get(i).getEdges();
//			Vector3f crossProd = new Vector3f();
//			crossProd.cross(edges[0].getEdgeValue(), edges[1].getEdgeValue());
//			if (crossProd.length() == 0.0 || edges[0].getEdgeValue().length() == 0.0 || 
//					edges[1].getEdgeValue().length() == 0.0 || edges[2].getEdgeValue().length() == 0.0) {
//				logger.debug("removing" + allTriangles.get(i));
//				List<Triangle> tn = new ArrayList<Triangle>();
//				tn.addAll(allTriangles.get(i).getNeighbors());
//				for (int j = 0 ; j < tn.size() ; ++j) {
//					tn.get(j).removeNeighbor(allTriangles.get(i));
//				}
//				model.getTriangles().remove(allTriangles.get(i));
//				allTriangles.remove(allTriangles.get(i));
//				rmTriangles++;
//			}
//		}
//		if (rmTriangles > 0) {
//			logger.debug("Removed " + rmTriangles + " triangles");
//		}
//	}
	
	/**
	 * KMeans algorithm implementation for vertex
	 * curvature classification
	 */
	public void KMeansVCClassification(HashMap<Vertex,Curvature> curvatures) {
		if (NUM_OF_CLUSTERS >= model.getVertices().size() / 4) {
			logger.debug("Number of vertices in the model smaller than number of clusters chosen");
			int temp = NUM_OF_CLUSTERS;
			NUM_OF_CLUSTERS = temp / 2;
			logger.debug("Number of clusters has been reduced from " +
			temp + " to " + NUM_OF_CLUSTERS);
		}
		Cluster[] clusters = new Cluster[NUM_OF_CLUSTERS];
		
		// randomly initialize clusters with one element each
		List<Integer> dummyRnd = new ArrayList<Integer>();
		final List<Integer> pickedData = new ArrayList<Integer>();
		List<Vertex> verticesData = new ArrayList<Vertex>(model.getVertices());
		for (int i = 0 ; i < verticesData.size() ; ++i) {
			dummyRnd.add(i);
		}
		Collections.shuffle(dummyRnd);
		pickedData.addAll(dummyRnd.subList(0, NUM_OF_CLUSTERS));
		for (int i = 0 ; i < NUM_OF_CLUSTERS ; ++i) {
			clusters[i] = new Cluster(i);
			// System.out.println(clusters[i].getLabelId());
			Collections.shuffle(dummyRnd);
			// store random picked element to cluster
			Vertex v = verticesData.get(pickedData.get(i));
			clusters[i].addVertexToCluster(v);
			clusters[i].updateCentroid(curvatures);
			v.setClusterLabel(clusters[i].getLabelId());
			v.setClusterCurvatureVal(clusters[i].getCentroid()[0],clusters[i].getCentroid()[1]);
		}
		
		// remove randomly picked elements
		for (int i = 0 ; i < NUM_OF_CLUSTERS ; ++i) {
			verticesData.remove(clusters[i].getVertices().get(0));
		}
		
		// add all remaining vertices to closest cluster according to Euclidean
		// distance applied on the curvature space
		while (!verticesData.isEmpty()) {
			Vertex v = verticesData.remove(0);
			float distMin = this.distEuclid(curvatures.get(v), clusters[0]);
			int clusterIndex = 0;
			for (int i = 1 ; i < NUM_OF_CLUSTERS ; ++i) {
				float distTmp = this.distEuclid(curvatures.get(v), clusters[i]);
				if (distTmp < distMin) {
					distMin = distTmp;
					clusterIndex = i;
				}
			}
			clusters[clusterIndex].addVertexToCluster(v);
			clusters[clusterIndex].updateCentroid(curvatures);
			v.setClusterLabel(clusters[clusterIndex].getLabelId());
			v.setClusterCurvatureVal(clusters[clusterIndex].getCentroid()[0],clusters[clusterIndex].getCentroid()[1]);
		}
		
//		for (int i = 0 ; i < NUM_OF_CLUSTERS ; ++i) {
//			System.out.println("Cluster Id " + clusters[i].getLabelId());
//			System.out.println("Vertices: " + clusters[i].getVertices());
//		}
		
		// iterate until no significant changes are visible or iteration
		// limit is exceeded
		boolean isRunning = true;
		int iteration = 0;
		while (isRunning && iteration < UPPER_ITERATION_LIMIT) {
			isRunning = false;
			for (int i = 0 ; i < model.getVertices().size() ; ++i) {
				float distMin = this.distEuclid(curvatures.get(model.getVertices().get(i)), clusters[0]);
				int clusterIndex = 0;
				for (int j = 1 ; j < NUM_OF_CLUSTERS ; ++j) {
					float distTmp = this.distEuclid(curvatures.get(model.getVertices().get(i)), clusters[j]);
					if (distTmp < distMin) {
						distMin = distTmp;
						clusterIndex = j;
					}
				}
				//System.out.println(model.getVertices().get(i));
				if (model.getVertices().get(i).getClusterLabel() != clusterIndex) {
					//System.out.println(model.getVertices().get(i).getClusterLabel());
					clusters[model.getVertices().get(i).getClusterLabel()].removeVertexFromCluster(model.getVertices().get(i));
					clusters[model.getVertices().get(i).getClusterLabel()].updateCentroid(curvatures);
					clusters[clusterIndex].addVertexToCluster(model.getVertices().get(i));
					clusters[clusterIndex].updateCentroid(curvatures);
					model.getVertices().get(i).setClusterLabel(clusterIndex);
					model.getVertices().get(i).setClusterCurvatureVal(clusters[clusterIndex].getCentroid()[0], clusters[clusterIndex].getCentroid()[1]);
					isRunning = true;
				}
			}
		}
		this.modelKMeansCurvatureClassification = true;
		
		int classifiedVertices = 0;
		for (int i = 0 ; i < NUM_OF_CLUSTERS ; ++i) {
			classifiedVertices += clusters[i].getVertices().size();
			for (int j = 0 ; j < clusters[i].getVertices().size() ; ++j) {
				Curvature c = curvatures.get(clusters[i].getVertices().get(j));
				clusters[i].getVertices().get(j).setClusterCurvatureVal(clusters[i].getCentroid()[0], clusters[i].getCentroid()[1]);
				c.setCurvatureMin(clusters[i].getCentroid()[0]);
				c.setCurvatureMax(clusters[i].getCentroid()[1]);
			}
		}
		CurvatureCalculation.setCurvatureHueSaturation(curvatures, model, 0.5f);
		
		for (int i = 0 ; i < NUM_OF_CLUSTERS ; ++i) {
			System.out.println(clusters[i]);
		}
		
		logger.debug("Classified " + classifiedVertices + " vertices out of " + model.getVertices().size() + " into " + NUM_OF_CLUSTERS + " clusters");
	}
	
	/**
	 * Computes the Euclidean distance between the centroid of a cluster
	 * and the curvature of a vertex
	 *  
	 * @param c
	 * 			curvature associated with selected vertex
	 * @param cluster
	 * 			cluster considered
	 * @return
	 * 			Euclidean distance between c and cluster.centroid
	 */
	private float distEuclid(Curvature c, Cluster cluster) {
		return (float)Math.sqrt(Math.pow(c.getCurvatureMin() - cluster.getCentroid()[0] , 2) 
				+ Math.pow(c.getCurvatureMax() - cluster.getCentroid()[1] , 2));
	}
	
	/**
	 * Implements the region growing block for the triangles
	 * inside the model. Should be called after the classification
	 * process has ended.
	 */
	public List<Region> processRegionGrowing() {
		List<Region> regions = new ArrayList<Region>();
		int contId = 0, cont = 0;
		List<Triangle> toLoop = new ArrayList<Triangle>();
		toLoop.addAll(model.getTriangles());
		for (Triangle tr : model.getTriangles()) {
			if (tr.updateIsSeedTriangle()) {
				cont++;
			}
		}
		logger.debug("seed tr. = " + cont);
		for (Triangle tr : model.getTriangles()) {
			if (tr.updateIsSeedTriangle() && tr.getRegionLabel() == -1) {
				Region newRegion = new Region(contId,tr);
				newRegion.buildUpRegion();
				regions.add(newRegion);
				contId++;
			}
		}
		if (regions.size() != 0) {
			int conti = 0;
			for (Triangle tr : model.getTriangles()) {
				if (tr.getRegionLabel() == -1) {
					//fillRegionCrackForTriangle(regions, tr);
					conti++;
				}
			}
			logger.debug("cont = " + conti);
		}
		else {
			for (Triangle tr : model.getTriangles()) {
				if (tr.getRegionLabel() == -1) {
					Region newRegion = new Region(contId,tr);
					newRegion.buildUpRegion();
					regions.add(newRegion);
					contId++;
				}
			}
		}
		return regions;
	}
	
	/**
	 * Crack filling recursive algorithm that assigns unlabelled 
	 * triangles to the maximally spread neighboring region of itself 
	 * or of its neighbors
	 * 
	 * @param regions
	 * 			built regions of the model mesh
	 * 
	 * @param tr
	 * 			triangle to be filled in one of the regions
	 */
	public void fillRegionCrackForTriangle(List<Region> regions, Triangle tr) {
		float maxArea = 0.0f;
		int regionLabelId = -1;
		for (Triangle neighbor : tr.getNeighbors()) {
			if (neighbor.getRegionLabel() == -1) {
				fillRegionCrackForTriangle(regions, tr);
			}
			if (maxArea < regions.get(neighbor.getRegionLabel()).getAreaOfRegion()) {
				maxArea = regions.get(neighbor.getRegionLabel()).getAreaOfRegion();
				regionLabelId = regions.get(neighbor.getRegionLabel()).getRegionId();
			}
		}
		regions.get(regionLabelId).addTriangleToRegion(tr);
	}
}
