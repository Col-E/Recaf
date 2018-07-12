/**
 * When the window loads, insert collapse buttons.
 */
window.onload = function() {
	// Iterate code blocks:
	var codeBlocks = document.getElementsByTagName("pre");
	for (var i = 0; i < codeBlocks.length; i++) {
		// Iterate block children to identify the sections.
		// They should be the only elements with ID's in the <pre> tag.
		var pre = codeBlocks[i];
		var children = pre.getElementsByTagName("*");
		for (var c = 0; c < children.length; c++) {
			var child = children[c];
			// ID span found, add collapse to it.
			if (!isEmpty(child.id)){
				addCollapse(pre, child.id);
				c++;
			}
		}
	}
};

/**
 * containerx: 
 *			Element that holds the container marked by the ID
 * id:
 *			ID of collapseable content. Relative to the container, not the whole document.
 */
function addCollapse(containerx, id) {
	// create buttons that collapse the content of those containers.
	var container = containerx.querySelector("#" + id);
	//var parent = container.parentNode;
	var toggleBtn = document.createElement("div");
	toggleBtn.className = "toggle-open";
	// click action
	toggleBtn.onclick = function () {
		collapse(toggleBtn, container, id);
	};
	// hover to show what will be collapsed
	toggleBtn.onmouseout = function () {
		setHighlighted(container, true);
		setHighlightedDummy(id, true);
	}
	toggleBtn.onmouseover = function () {
		setHighlighted(container, false);
		setHighlightedDummy(id, false);
	}
	containerx.insertBefore(toggleBtn, container);
}

/**
 * container: 
 *			Container to be marked.
 * highlighted:
 *			If container should be highlighted, noting collapseable content.
 */
function setHighlighted(container, highlighted) {
	var cls = container.className;
	if(highlighted) {
		container.className = container.className.replace("toggle-section", "");
	} else {
		container.className += " toggle-section";
	}
}

/**
 * id: 
 *			ID of dummy content to be marked.
 * highlighted:
 *			If container should be highlighted, noting collapseable content.
 */
function setHighlightedDummy(id, highlighted) {
	var dcID = "dummy-cont-" + id;
	var container = document.getElementById(dcID);
	if (container != null) {
		setHighlighted(container, highlighted);
	}
}

/**
 * button:
 *			The button that controls the visibility of the container.
 * container:
 *			Span containing collapsable content.
 * id:
 *			ID of the container.
 */
function collapse(button, container, id) {
	// toggle button format
	var btnType = button.className;
	var dlID = "dummy-line-" + id;
	var dcID = "dummy-cont-" + id; 
	if(btnType == "toggle-open") {
		button.className = "toggle-closed";
		// replace with dummy line
		var dLine = document.createElement("span");
		var dContent = document.createElement("span");
		dLine.id = dlID;
		dLine.className = "line";
		dContent.id = dcID; 
		dContent.className = "comment-line toggle-section"; // selection included since mouse will be over
		dContent.innerHTML = "// " + id + "... ";
		container.parentNode.insertBefore(dLine, container);
		container.parentNode.insertBefore(dContent, container);
	} else{
		button.className = "toggle-open";
		// remove dummy items
		document.getElementById(dlID).outerHTML = "";
		document.getElementById(dcID).outerHTML = "";
	}
	// toggle visibility of container
	if (container.style.display === "none") {
		container.style.display = "inline";
	} else {
		container.style.display = "none";
	}
}

/**
 * Check if string is empty.
 */
function isEmpty(str) {
	return (!str || 0 === str.length);
}