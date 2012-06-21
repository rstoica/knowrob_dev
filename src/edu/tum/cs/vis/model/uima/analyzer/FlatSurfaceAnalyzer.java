package edu.tum.cs.vis.model.uima.analyzer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Vector3d;

import org.apache.log4j.Logger;

import edu.tum.cs.vis.model.uima.annotation.FlatSurfaceAnnotation;
import edu.tum.cs.vis.model.uima.cas.MeshCas;
import edu.tum.cs.vis.model.util.Group;
import edu.tum.cs.vis.model.util.Mesh;
import edu.tum.cs.vis.model.util.Polygon;

/**
 * Takes a MeshCas and searches all flat surfaces by comparing the surface normals. Flat means the
 * surface normals are equal (or exactly the opposite) or within a given tolerance.
 * 
 * @author Stefan Profanter
 * 
 */
public class FlatSurfaceAnalyzer extends MeshAnalyzer {

	private static Logger				logger				= Logger.getLogger(FlatSurfaceAnalyzer.class);

	/**
	 * Allowed tolerance between surface normals considered as equal. Tolerance is in radiant.
	 */
	static final double					TOLERANCE			= 1.0 * Math.PI / 180;

	private final List<Callable<Void>>	threads				= new LinkedList<Callable<Void>>();

	private ArrayList<Polygon>			allPolygons;
	private final AtomicInteger			polygonsElaborated	= new AtomicInteger(0);

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public String getName() {
		return "FlatSurface";
	}

	/**
	 * Do a BFS on the neighbors of the <code>start</code> polygon and find all neighbors with
	 * nearly the same surface normal.
	 * 
	 * @param start
	 *            Polygon with reference normal
	 * @param alreadyInAnnotation
	 *            List of polynom which are already in a annotaion. So they form already a flat
	 *            surface and don't need to be checked again.
	 * @param cas
	 */
	void polygonBFS(Polygon start, LinkedList<Polygon> alreadyInAnnotation, MeshCas cas) {
		if (alreadyInAnnotation.contains(start))
			return;

		LinkedList<Polygon> visited = new LinkedList<Polygon>();

		alreadyInAnnotation.add(start);
		visited.add(start);

		LinkedList<Polygon> queue = new LinkedList<Polygon>();
		if (start.getNeighbors() != null)
			queue.addAll(start.getNeighbors());

		Vector3d inv = (Vector3d) start.getNormalVector().clone();
		inv.scale(-1);

		FlatSurfaceAnnotation annotation = new FlatSurfaceAnnotation();
		annotation.getMesh().setTextureBasePath(cas.getGroup().getMesh().getTextureBasePath());
		annotation.getMesh().getPolygons().add(start);

		while (!queue.isEmpty()) {
			Polygon p = queue.pop();
			visited.add(p);

			// First check if sufrace normal is exactly the same direction
			boolean isEqual = (p.getNormalVector().equals(start.getNormalVector()) || p
					.getNormalVector().equals(inv));

			// if not, use angle between normals
			if (!isEqual) {
				/*
				 * Calculate angle (range: 0 - PI) between the two surface normals. Because due to floating point arithmetic there may be
				 * small errors which can be compensated by checking the angle
				 */
				double radiant = Math.acos(start.getNormalVector().dot(p.getNormalVector()));
				isEqual = (radiant < FlatSurfaceAnalyzer.TOLERANCE)
						|| (Math.PI - radiant < FlatSurfaceAnalyzer.TOLERANCE);
			}

			if (isEqual && !alreadyInAnnotation.contains(p)) {
				annotation.getMesh().getPolygons().add(p);
				alreadyInAnnotation.add(p);
				for (Polygon a : p.getNeighbors()) {
					if (alreadyInAnnotation.contains(a) || visited.contains(a) || queue.contains(a))
						continue;
					queue.add(a);
				}
			}
		}
		annotation.setFeatures();
		cas.addAnnotation(annotation);
	}

	/**
	 * Process the mesh of the group <code>g</code> with <code>processMesh</code> and all child
	 * groups.
	 * 
	 * @param g
	 *            group to process
	 * @param cas
	 *            CAS to add a new annotation if flat surface is found.
	 */
	private void processGroup(Group g, MeshCas cas) {
		processMesh(g.getMesh(), cas);
		for (Group gr : g.getChildren()) {
			processGroup(gr, cas);
		}
	}

	/**
	 * Process all polygons in the given mesh <code>m</code> with <code>polygonBFS</code>
	 * 
	 * @param m
	 *            mesh to process
	 * @param cas
	 *            CAS to add a new annotation if flat surface is found.
	 */
	private void processMesh(Mesh m, final MeshCas cas) {

		if (m.getPolygons().size() == 0)
			return;

		allPolygons.addAll(m.getPolygons());
	}

	@Override
	public void processStart(MeshCas cas) {
		FlatSurfaceAnnotation fsa = new FlatSurfaceAnnotation();
		fsa.setMesh(cas.getGroup().getMesh().getInitializedChildMesh());

		allPolygons = new ArrayList<Polygon>();

		processGroup(cas.getGroup(), cas);

		// Iterate over the polygons and find neighbors with the same normal vector
		final LinkedList<Polygon> alreadyInAnnotation = new LinkedList<Polygon>();

		for (Polygon tr : allPolygons) {
			if (alreadyInAnnotation.contains(tr)) {
				polygonsElaborated.incrementAndGet();
				continue;
			}
			polygonBFS(tr, alreadyInAnnotation, cas);
			polygonsElaborated.incrementAndGet();
		}
	}

	@Override
	public void updateProgress() {
		if (allPolygons != null)
			setProgress((float) polygonsElaborated.get() / (float) allPolygons.size() * 100.0f);
	}
}
