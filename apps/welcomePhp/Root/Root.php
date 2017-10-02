class Root {

	public function main_action() {
		$param = new HopObject("HopObject")->toJavaObject();
		
		$param->content = "";
		for ($i = 0; $i < $this->size(); $i++) {
			$param->content .= $this->get($i)->renderSkinAsString("content", new HopObject("HopObject")->toJavaObject());
		}
		
		renderSkin("global", $param);
	}
	
	public function add_action() {
		$param = new HopObject("HopObject")->toJavaObject();
		
		$param->content = $this->renderSkinAsString("contentAdd", new HopObject("HopObject")->toJavaObject());
		for ($i = 0; $i < $this->size(); $i++) {
			$param->content .= $this->get($i)->renderSkinAsString("content", new HopObject("HopObject")->toJavaObject());
		}
		
		renderSkin("global", $param);
	}
	
	public function save_action() {
		global $res;
		global $req;
		global $root;
		global $session;
		
		if ($req->get("title") && $req->get("text")) {
			$content = new Content("Content")->toJavaObject();
			$content->title = $req->get("title");
			$content->text = $req->get("text");
			$root->add($content);
			
			$session->getData()->setString("message", "Added.");
			$res->redirect($this->href());
		}
		
		$session->getData()->setString("message", "Some fields were empty!"); 
		$res->redirect($this->href("add"));
	}

}