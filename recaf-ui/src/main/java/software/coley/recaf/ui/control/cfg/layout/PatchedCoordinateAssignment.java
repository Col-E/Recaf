// Copyright GFI 2017 - Data Systemizer
package software.coley.recaf.ui.control.cfg.layout;

import com.mxgraph.layout.hierarchical.model.mxGraphAbstractHierarchyCell;
import com.mxgraph.layout.hierarchical.model.mxGraphHierarchyEdge;
import com.mxgraph.layout.hierarchical.model.mxGraphHierarchyModel;
import com.mxgraph.layout.hierarchical.model.mxGraphHierarchyNode;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.hierarchical.stage.mxCoordinateAssignment;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Patched hierarchical layout to route directly
 * cross-group edges
 *
 * @author Loison
 */
public class PatchedCoordinateAssignment extends mxCoordinateAssignment {

  /**
   * Constructor
   *
   * @param layout
   * @param intraCellSpacing
   * @param interRankCellSpacing
   * @param orientation
   * @param initialX
   * @param parallelEdgeSpacing
   */
  public PatchedCoordinateAssignment(mxHierarchicalLayout layout, double intraCellSpacing, double interRankCellSpacing,
                                     int orientation, double initialX, double parallelEdgeSpacing) {
    super(layout, intraCellSpacing, interRankCellSpacing, orientation, initialX, parallelEdgeSpacing);
  }

  /**
   * Sets the cell locations in the facade to those
   * stored after this layout
   * processing step has completed.
   *
   * @param graph the facade describing the input graph
   * @param model an internal model of the hierarchical
   *              layout
   */
  @Override
  protected void setCellLocations(mxGraph graph, mxGraphHierarchyModel model) {
    rankTopY = new double[model.ranks.size()];
    rankBottomY = new double[model.ranks.size()];

    for (int i = 0; i < model.ranks.size(); i++) {
      rankTopY[i] = Double.MAX_VALUE;
      rankBottomY[i] = -Double.MAX_VALUE;
    }

    Set<Object> parentsChanged = null;

    if (layout.isResizeParent()) {
      parentsChanged = new HashSet<>();
    }

    Map<Object, mxGraphHierarchyEdge> edges = model.getEdgeMapper();
    Map<Object, mxGraphHierarchyNode> vertices = model.getVertexMapper();

    // Process vertices all first, since they define the
    // lower and
    // limits of each rank. Between these limits lie the
    // channels
    // where the edges can be routed across the graph

    for (mxGraphHierarchyNode cell : vertices.values()) {
      setVertexLocation(cell);

      if (layout.isResizeParent()) {
        parentsChanged.add(graph.getModel().getParent(cell.cell));
      }
    }

    if (layout.isResizeParent()) {
      adjustParents(parentsChanged);
    }

    // MODIF FLO : enum is not visible

    // Post process edge styles. Needs the vertex
    // locations set for initial
    // values of the top and bottoms of each rank
    //if (this.edgeStyle == HierarchicalEdgeStyle.ORTHOGONAL
    //        || this.edgeStyle == HierarchicalEdgeStyle
    //        .POLYLINE)
    //{
    localEdgeProcessing(model);
    //}

    // MODIF FLO : remove jetty and ranks for
    // cross-groups edges : they are garbled

    for (mxGraphAbstractHierarchyCell cell : edges.values()) {
      mxGraphHierarchyEdge edge = (mxGraphHierarchyEdge) cell;

      // Cross group edge?
      boolean isCrossGroupEdge = isCrossGroupEdge(edge);

      if (isCrossGroupEdge) {

        // Clear jettys
        this.jettyPositions.remove(edge);

        // Clear min and max ranks
        edge.minRank = -1;
        edge.maxRank = -1;

      }

    }
    // end MODIF FLO
    for (mxGraphAbstractHierarchyCell cell : edges.values()) {
      setEdgePosition(cell);
    }
  }

  public boolean isCrossGroupEdge(mxGraphHierarchyEdge edge) {

    // Cross group edge?
    boolean isCrossGroupEdge = false;

    for (Object objCell : edge.edges) {

      mxCell edgeCell = (mxCell) objCell;

      // Edge parent same as source parent?
      isCrossGroupEdge = isCrossGroupEdge || (!edgeCell.getParent().equals(edgeCell.getSource().getParent()));
      // Edge parent same as target parent?
      isCrossGroupEdge = isCrossGroupEdge || (!edgeCell.getParent().equals(edgeCell.getTarget().getParent()));

      if (isCrossGroupEdge) {
        // Finished
        break;
      }

    }
    return isCrossGroupEdge;
  }
}
