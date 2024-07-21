// Copyright GFI 2017 - Data Systemizer
package software.coley.recaf.ui.control.cfg.layout;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.hierarchical.stage.mxCoordinateAssignment;
import com.mxgraph.view.mxGraph;

/**
 * Patched hierarchical layout to route directly
 * cross-group edges
 *
 * @author Loison
 */
public class PatchedHierarchicalLayout extends mxHierarchicalLayout {

  public PatchedHierarchicalLayout(mxGraph graph) {
    super(graph);
  }

  public PatchedHierarchicalLayout(mxGraph graph, int orientation) {
    super(graph, orientation);
  }

  /**
   * Executes the placement stage using
   * mxCoordinateAssignment.
   * <p/>
   * Use a patched mxCoordinateAssignment class
   */
  @Override
  public double placementStage(double initialX, Object parent) {
    mxCoordinateAssignment placementStage =
            new PatchedCoordinateAssignment(this, intraCellSpacing, interRankCellSpacing, orientation, initialX,
                    parallelEdgeSpacing);
    placementStage.setFineTuning(fineTuning);
    placementStage.execute(parent);

    return placementStage.getLimitX() + interHierarchySpacing;
  }

}
