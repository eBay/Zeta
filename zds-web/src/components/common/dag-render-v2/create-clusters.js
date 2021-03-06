var _ = require("lodash"),
    d3 = require("d3"),
    addLabel = require("./add-label"),
    util = require("../../../../node_modules/dagre-d3/lib/util");

module.exports = createClusters;

function createClusters(selection, g) {
  var clusters = g.nodes().filter(function(v) { return util.isSubgraph(g, v); }),
      svgClusters = selection.selectAll("g.cluster")
        .data(clusters, function(v) { return v; });

  svgClusters.selectAll("*").remove();
  svgClusters.enter()
    .append("g")
      .attr("class", function(v){
		var className =  g.node(v).class ? v + ' ' + g.node(v).class : v;
        if(v.indexOf('cluster') >= 0 ){
          className += ' cluster';
        }
        if(v.indexOf('stage') >= 0 ){
          className += ` cluster stage`;

		}
        return className

    })
      .attr("id",function(v){
          var node = g.node(v);
          return node.id;
      })
      .style("opacity", 0);
  svgClusters.attr("class", function(v){
      var className =  g.node(v).class ? v + ' ' + g.node(v).class : v;
      if(v.indexOf('cluster') >= 0 ){
        className += ' cluster';
      }
      if(v.indexOf('stage') >= 0 ){
        className += ` cluster stage`;

      }
      return className
  });
  svgClusters = selection.selectAll("g.cluster");

  util.applyTransition(svgClusters, g)
    .style("opacity", 1);

  svgClusters.each(function(v) {
    var node = g.node(v),
        thisGroup = d3.select(this);
    d3.select(this).append("rect");
    var labelGroup = thisGroup.append("g").attr("class", "label");
    addLabel(labelGroup, node, node.clusterLabelPos);


  });


  svgClusters.selectAll("rect").each(function(c) {
    var node = g.node(c);
    var domCluster = d3.select(this);
    util.applyStyle(domCluster, node.style);
  });

  var exitSelection;

  if (svgClusters.exit) {
    exitSelection = svgClusters.exit();
  } else {
    exitSelection = svgClusters.selectAll(null); // empty selection
  }

  util.applyTransition(exitSelection, g)
    .style("opacity", 0)
    .remove();

  return svgClusters;
}
