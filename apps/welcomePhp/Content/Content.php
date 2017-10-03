//<?php

class Content {

  public function edit_action() {
    global $root;

    $params = ["content" => ""];
    for ($i = 0; $i < $root->size(); $i++) {
      if ($root->get($i)->equals($this)) {
        $params["content"] .= $root->get($i)->renderSkinAsString("contentEdit");
      } else {
        $params["content"] .= $root->get($i)->renderSkinAsString("content");
      }
    }

    renderSkin("global", $params);
  }

  public function save_action() {
    global $res;
    global $req;
    global $root;
    global $session;

    if ($req->get("title") && $req->get("text")) {
      $this->title = $req->get("title");
      $this->text = $req->get("text");

      $session->getData()->setString("message", "Saved.");
      $res->redirect($root->href());
    }

    $session->getData()->setString("message", "Some fields were empty!");
    $res->redirect($this->href("edit"));
  }

  public function delete_action() {
    global $session;
    global $root;
    global $res;

    $this->remove();
    $session->getData()->setString("message", "Deleted.");
    $res->redirect($root->href());
  }

}
