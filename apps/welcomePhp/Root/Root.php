//<?php

class Root {

  public function main_action() {
    $params = ["content" => ""];
    for ($i = 0; $i < $this->size(); $i++) {
      $params["content"] .= $this->get($i)->renderSkinAsString("content");
    }

    renderSkin("global", $params);
  }

  public function add_action() {
    $params = ["content" => $this->renderSkinAsString("contentAdd")];
    for ($i = 0; $i < $this->size(); $i++) {
      $param["content"] .= $this->get($i)->renderSkinAsString("content");
    }

    renderSkin("global", $params);
  }

  public function save_action() {
    global $res;
    global $req;
    global $root;
    global $session;

    if ($req->get("title") && $req->get("text")) {
      $content = new Content();
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
