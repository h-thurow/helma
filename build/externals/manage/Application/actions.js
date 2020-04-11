/**
* renders AppManager
*/
function main_action() {
    if (checkAddress() == false)
        return;
    if (checkAuth(this) == false)
        return;

    res.data.body = this.renderSkinAsString("main");
    renderSkin("global");
}


/**
* prints session- and thread-stats for mrtg-tool
* doesn't check username or password, so that we don't have
* to write them cleartext in a mrtg-configfile but checks the
* remote address.
*/
function mrtg_action() {
    if (checkAddress() == false)
        return;

    if (this.isActive() == false) {
        res.write("0\n0\n0\n0\n");
        return;
    }

    if (req.data.action == "sessions") {

        res.write(this.sessions.size());
        res.write("\n0\n0\n0\n");

    } else if (req.data.action == "threads") {

        res.write(this.countActiveEvaluators() + "\n");
        res.write(this.countEvaluators() + "\n");
        res.write("0\n0\n");

    } else if (req.data.action == "cache") {

        res.write(this.getCacheUsage() + "\n");
        res.write(this.getProperty("cachesize", "1000") + "\n");
        res.write("0\n0\n");

    } else if (req.data.action == "requests") {

        //   res.write (

    } else {
        res.write("0\n0\n0\n0\n");
    }

}

/**
* performs a redirect to the public site
* (workaround, we can't access application object from docapplication for some reason)
* @see application.url_macro
*/
function redirectpublic_action() {
    if (checkAddress() == false)    return;
    if (checkAuth(this) == false)    return;

    res.redirect(this.url_macro());
}
