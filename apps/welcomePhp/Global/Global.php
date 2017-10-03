//<?php

function message_macro() {
  global $session;

  $message = $session->getData()->get("message");
  $session->getData()->unset("message");
  return $message;
}
