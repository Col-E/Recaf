// When the user clicks on the button, open the modal 
Element.prototype.remove = function() {
    this.parentElement.removeChild(this);
}
function showModal(source) {
	var modal = document.createElement("div");
	modal.className = "modal";
	modal.id = "splashModal";
	modal.tabIndex = "-1";
	modal.addEventListener("click", function(){
		document.getElementById("splashModal").remove();
	}, false);
	
	var content = document.createElement("div");
	content.className = "modal-content";
	
	var image = document.createElement("img");
	image.src = source.src;
	
	document.body.appendChild(modal);
	modal.appendChild(content);
	content.appendChild(image);
	modal.focus();
}