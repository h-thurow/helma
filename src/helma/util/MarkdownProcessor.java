package helma.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MarkdownProcessor {

    private HashMap links = new HashMap();
    private int state;
    private int i;
    private int length;
    private char[] chars;
    private StringBuilder buffer;
    private int lineMarker = 0;
    private int paragraphStartMarker = 0;
    private boolean listParagraphs = false;
    private int codeEndMarker = 0;
    private ElementStack stack = new ElementStack();
    private Emphasis[] emph = new Emphasis[2];

    private String result = null;

    // private Logger log = Logger.getLogger(MarkdownProcessor.class);
    private int line;

    private final static int
        // stage 1 states
        NONE = 0, NEWLINE = 1, LINK_ID = 2, LINK_URL = 3,
        // stage 2 states
        HEADER = 4, PARAGRAPH = 5, LIST = 6, HTML_BLOCK = 7, CODE = 8;

    static final Set blockTags = new HashSet();

    static {
        blockTags.add("p"); //$NON-NLS-1$
        blockTags.add("div"); //$NON-NLS-1$
        blockTags.add("h1"); //$NON-NLS-1$
        blockTags.add("h2"); //$NON-NLS-1$
        blockTags.add("h3"); //$NON-NLS-1$
        blockTags.add("h4"); //$NON-NLS-1$
        blockTags.add("h5"); //$NON-NLS-1$
        blockTags.add("h6"); //$NON-NLS-1$
        blockTags.add("blockquote"); //$NON-NLS-1$
        blockTags.add("pre"); //$NON-NLS-1$
        blockTags.add("table"); //$NON-NLS-1$
        blockTags.add("tr"); // handle <tr> as block tag for pragmatical reasons //$NON-NLS-1$
        blockTags.add("dl"); //$NON-NLS-1$
        blockTags.add("ol"); //$NON-NLS-1$
        blockTags.add("ul"); //$NON-NLS-1$
        blockTags.add("script"); //$NON-NLS-1$
        blockTags.add("noscript"); //$NON-NLS-1$
        blockTags.add("form"); //$NON-NLS-1$
        blockTags.add("fieldset"); //$NON-NLS-1$
        blockTags.add("iframe"); //$NON-NLS-1$
        blockTags.add("math"); //$NON-NLS-1$
    }

    public MarkdownProcessor() {}

    public MarkdownProcessor(String text) {
        init(text);
    }

    public MarkdownProcessor(File file) throws IOException {
        this.length = (int) file.length();
        this.chars = new char[this.length + 2];
        FileReader reader = new FileReader(file);
        int read = 0;
        try {
            while (read < this.length) {
                int r = reader.read(this.chars, read, this.length - read);
                if (r == -1)
                    break;
                read += r;
            }
        } finally {
            reader.close();
        }
        this.length = read;
        this.chars[this.length] = this.chars[this.length + 1] = '\n';
    }

    public synchronized String process(String text) {
        init(text);
        return process();
    }

    public synchronized String process() {
        if (this.result == null) {
            this.length = this.chars.length;
            firstPass();
            secondPass();
            this.result = this.buffer.toString();
            cleanup();
        }
        return this.result;
    }

    public synchronized String processLinkText(String text) {
        init(text);
        return processLinkText();
    }

    private void init(String text) {
        this.length = text.length();
        this.chars = new char[this.length + 2];
        text.getChars(0, this.length, this.chars, 0);
        this.chars[this.length] = this.chars[this.length + 1] = '\n';
    }

   /**
    * Retrieve a link defined in the source text. If the link is not found, we call
    * lookupLink(String) to retrieve it from an external source.
    * @param linkId the link id
    * @return a String array with the url as first element and the link title as second.
    */
    protected String[] getLink(String linkId) {
        String[] link =  (String[]) this.links.get(linkId);
        if (link == null) {
            link = lookupLink(linkId);
        }
        return link;
    }

    /**
     * Method to override for extended link lookup, e.g. for integration into a wiki
     * @param linkId the link id
     * @return a String array with the url as first element and the link title as second.
     */
    protected String[] lookupLink(String linkId) {
        return null;
    }

    /**
     * Method to override to create custom HTML tags.
     * @param tag the html tag to generate
     * @param builder the java.lang.StringBuilder to generate the string
     */
    protected void openTag(String tag, StringBuilder builder) {
        builder.append('<').append(tag).append('>');
    }

    /**
     * First pass: extract links definitions and remove them from the source text.
     */
    private synchronized void firstPass() {
        int state = MarkdownProcessor.NEWLINE;
        int linestart = 0;
        int indentation = 0;
        int indentationChars = 0;
        String linkId = null;
        String[] linkValue = null;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < this.length; i++) {
            // convert \r\n and \r newlines to \n
            if (this.chars[i] == '\r') {
                if (i < this.length && this.chars[i + 1] == '\n') {
                    System.arraycopy(this.chars, i + 1, this.chars, i, (this.length - i) - 1);
                    this.length -= 1;
                } else {
                    this.chars[i] = '\n';
                }
            }
        }

        for (int i = 0; i < this.length; i++) {
            char c = this.chars[i];

            switch (state) {
                case MarkdownProcessor.NEWLINE:
                    if (c == '[' && indentation < 4) {
                        state = MarkdownProcessor.LINK_ID;
                    } else if (isSpace(c)) {
                        indentationChars += 1;
                        indentation += (c == '\t') ? 4 : 1;
                    } else if (c == '\n' && indentationChars > 0) {
                        System.arraycopy(this.chars, i, this.chars, i - indentationChars, this.length - i);
                        i -= indentationChars;
                        this.length -= indentationChars;
                    } else {
                        state = MarkdownProcessor.NONE;
                    }
                    break;
                case MarkdownProcessor.LINK_ID:
                    if (c == ']') {
                        if (i < this.length - 1 && this.chars[i + 1] == ':') {
                            linkId = buffer.toString();
                            linkValue = new String[2];
                            state = MarkdownProcessor.LINK_URL;
                            i++;
                        } else {
                            state = MarkdownProcessor.NONE;
                        }
                        buffer.setLength(0);
                    } else {
                        buffer.append(c);
                    }
                    break;
                case MarkdownProcessor.LINK_URL:
                    if (c == '<' && buffer.length() == 0) {
                        continue;
                    } else if ((Character.isWhitespace(c) || c == '>') && buffer.length() > 0) {
                        linkValue[0] = buffer.toString().trim();
                        buffer.setLength(0);
                        int j = i + 1;
                        int newlines = c == '\n' ? 1 : 0;
                        while (j < this.length && Character.isWhitespace(this.chars[j])) {
                            if (this.chars[j] == '\n') {
                                newlines += 1;
                                if (newlines > 1) {
                                    break;
                                }
                                i = j;
                                c = this.chars[j];
                            }
                            j += 1;
                        }
                        if (j < this.length && newlines <= 1 && isLinkQuote(this.chars[j])) {
                            char quoteChar = this.chars[j] == '(' ? ')' : this.chars[j];
                            int start = j = j + 1;
                            int len = -1;
                            while (j < this.length && this.chars[j] != '\n') {
                                if (this.chars[j] == quoteChar) {
                                    len = j - start;
                                } else if (len > -1 && !isSpace(this.chars[j])) {
                                    len = -1;
                                }
                                j += 1;
                            }
                            if (len > -1) {
                                linkValue[1] = new String(this.chars, start, len);
                                i = j;
                                c = this.chars[j];
                            }
                        }
                        if (c == '\n' && state != MarkdownProcessor.NONE) {
                            this.links.put(linkId.toLowerCase(), linkValue);
                            System.arraycopy(this.chars, i, this.chars, linestart, this.length - i);
                            this.length -= (i - linestart);
                            i = linestart;
                            buffer.setLength(0);
                            linkId = null;
                        } else {
                            // no valid link title - escape
                            state = MarkdownProcessor.NONE;
                        }
                    } else if (!isSpace(c) || buffer.length() > 0) {
                        buffer.append(c);
                    }
            }

            if (c == '\n') {
                state = MarkdownProcessor.NEWLINE;
                linestart = i;
                indentation = indentationChars = 0;
            }

        }
    }

    private synchronized void secondPass() {
        this.state = MarkdownProcessor.NEWLINE;
        this.stack.add(new BaseElement());
        this.buffer = new StringBuilder((int) (this.length * 1.2));
        this.line = 1;
        boolean escape = false;

        for (this.i = 0; this.i < this.length; ) {
            char c;

            if (this.state == MarkdownProcessor.NEWLINE) {
                checkBlock(0);
            }

            boolean leadingSpaceChars = true;

            while (this.i < this.length && this.chars[this.i] != '\n') {

                c = this.chars[this.i];
                leadingSpaceChars = leadingSpaceChars && isSpace(c);

                if (this.state == MarkdownProcessor.HTML_BLOCK) {
                    this.buffer.append(c);
                    this.i += 1;
                    continue;
                }

                if (escape) {
                    this.buffer.append(c);
                    escape = false;
                    this.i += 1;
                    continue;
                } else if (c == '\\') {
                    escape = true;
                    this.i += 1;
                    continue;
                }

                switch (c) {
                    case '*':
                    case '_':
                        if (checkEmphasis(c)) {
                            continue;
                        }
                        break;

                    case '`':
                        if (checkCodeSpan(c)) {
                            continue;
                        }
                        break;

                    case '[':
                        if (checkLink(c)) {
                            continue;
                        }
                        break;

                    case '!':
                        if (checkImage()) {
                            continue;
                        }
                        break;

                    case '<':
                        if (checkHtmlLink(c)) {
                            continue;
                        }
                        break;
                }

                if (this.state == MarkdownProcessor.HEADER) {
                    if (c == '#') {
                        ((Element) this.stack.peek()).mod++;
                    } else {
                        ((Element) this.stack.peek()).mod = 0;
                    }
                }

                if (!leadingSpaceChars) {
                    this.buffer.append(c);
                }
                this.i += 1;

            }

            while (this.i < this.length && this.chars[this.i] == '\n') {

                c = this.chars[this.i];
                this.line += 1;

                if (this.state == MarkdownProcessor.HTML_BLOCK &&
                        (this.i >= this.length - 1 || this.chars[this.i + 1] != '\n')) {
                    this.buffer.append(c);
                    this.i += 1;
                    continue;
                }
                if (this.state == MarkdownProcessor.HEADER) {
                    Element header = (Element) this.stack.pop();
                    if (header.mod > 0) {
                        this.buffer.setLength(this.buffer.length() - header.mod);
                    }
                    header.close();
                }

                int bufLen = this.buffer.length();
                boolean markParagraph = bufLen > 0 && this.buffer.charAt(bufLen - 1) == '\n';

                if (this.state == MarkdownProcessor.LIST && this.i < this.length) {
                    checkParagraph(this.listParagraphs);
                }
                if (this.state == MarkdownProcessor.PARAGRAPH && this.i < this.length) {
                    checkParagraph(true);
                    checkHeader();
                }

                this.buffer.append(c);
                this.state = MarkdownProcessor.NEWLINE;
                this.lineMarker = this.buffer.length();

                if (markParagraph) {
                    this.paragraphStartMarker = this.lineMarker;
                }
                this.i += 1;

            }

        }
        while (!this.stack.isEmpty()) {
            ((Element) this.stack.pop()).close();
        }
    }

    private boolean checkBlock(int blockquoteNesting) {
        int indentation = 0;
        int j = this.i;
        while (j < this.length && isSpace(this.chars[j])) {
            indentation += this.chars[j] == '\t' ? 4 : 1;
            j += 1;
        }

        if (j < this.length) {
            char c = this.chars[j];

            if (checkBlockquote(c, j, indentation, blockquoteNesting)) {
                return true;
            }

            if (checkCodeBlock(c, j, indentation, blockquoteNesting)) {
                return true;
            }

            if (checkList(c, j, indentation, blockquoteNesting)) {
                return true;
            }

            if (checkAtxHeader(c, j)) {
                return true;
            }

            if (!checkHtmlBlock(c, j)) {
                this.state = this.stack.search(ListElement.class) != null ? MarkdownProcessor.LIST : MarkdownProcessor.PARAGRAPH;
            }
        }
        return false;
    }

    private boolean checkEmphasis(char c) {
        for (int l = 1; l >= 0; l--) {
            if (this.emph[l] != null && this.emph[l].end == this.i) {
                this.emph[l].close();
                this.i += this.emph[l].mod;
                this.emph[l] = null;
                return true;
            }
        }
        if (c == '*' || c == '_') {
            int n = 1;
            int j = this.i + 1;
            while(j < this.length && this.chars[j] == c && n <= 3) {
                n += 1;
                j += 1;
            }
            int found = n;
            boolean isStartTag = j < this.length  - 1 && !Character.isWhitespace(this.chars[j]);
            if (isStartTag && (this.emph[0] == null || this.emph[1] == null)) {
                List possibleEndTags = new ArrayList();
                char lastChar = 0;
                int count = 0;
                boolean escape = false;
                for (int k = j; k <= this.length; k++) {
                    if (this.chars[k] == '\n' && lastChar == '\n') {
                        break;
                    }
                    lastChar = this.chars[k];

                    if (escape) {
                        escape = false;
                    } else {
                        if (this.chars[k] == '\\') {
                            escape = true;
                        } else if (this.chars[k] == '`') {
                            k = skipCodeSpan(k);
                        } else if (this.chars[k] == '[') {
                            k = skipLink(k);
                        } else if (this.chars[k] == c) {
                            count += 1;
                        } else {
                            if (count > 0 && !Character.isWhitespace(this.chars[k - count - 1])) {
                                // add an int array to possible end tags: [position, nuberOfTokens]
                                possibleEndTags.add(new int[] {k - count, count});
                            }
                            count = 0;
                        }
                    }
                }

                for (int l = 1; l >= 0; l--) {
                    if (this.emph[l] == null && n > l) {
                        this.emph[l] = checkEmphasisInternal(l + 1, possibleEndTags);
                        if (this.emph[l] != null) {
                            n -= l + 1;
                        }
                    }
                }
            }
            if (n == found) {
                return false;
            }
            // write out remaining token chars
            for (int m = 0; m < n; m++) {
                this.buffer.append(c);
            }
            this.i = j;
            return true;
        }
        return false;
    }

    private Emphasis checkEmphasisInternal(int length, List possibleEndTags) {
        for (int k = 0; k < possibleEndTags.size(); k++) {
            int[] possibleEndTag = (int[]) possibleEndTags.get(k);
            if (possibleEndTag[1] >= length) {
                Emphasis elem = new Emphasis(length, possibleEndTag[0]);
                elem.open();
                possibleEndTag[0] += length;
                possibleEndTag[1] -= length;
                return elem;
            }
        }
        return null;
    }

    private boolean checkCodeSpan(char c) {
        if (c != '`') {
            return false;
        }
        int n = 0; // additional backticks to match
        int j = this.i + 1;
        StringBuffer code = new StringBuffer();
        while(j < this.length && this.chars[j] == '`') {
            n += 1;
            j += 1;
        }
        outer: while(j < this.length) {
            if (this.chars[j] == '`') {
                if (n == 0) {
                    break;
                }
                if (j + n >= this.length) {
                    return false;
                }
                for (int k = j + 1; k <= j + n; k++) {
                    if (this.chars[k] != '`') {
                        break;
                    } else if (k == j + n) {
                        j = k;
                        break outer;
                    }
                }
            }
            if (this.chars[j] == '&') {
                code.append("&amp;"); //$NON-NLS-1$
            } else if (this.chars[j] == '<') {
                code.append("&lt;"); //$NON-NLS-1$
            } else if (this.chars[j] == '>') {
                code.append("&gt;"); //$NON-NLS-1$
            } else {
                code.append(this.chars[j]);
            }
            j += 1;
        }
        openTag("code", this.buffer); //$NON-NLS-1$
        this.buffer.append(code.toString().trim()).append("</code>"); //$NON-NLS-1$
        this.i = j + 1;
        return true;
    }

    // find the end of a code span starting at start
    private int skipCodeSpan(int start) {
        int n = 0; // additional backticks to match
        int j = start + 1;
        while(j < this.length && this.chars[j] == '`') {
            n += 1;
            j += 1;
        }
        outer: while(j < this.length) {
            if (this.chars[j] == '`') {
                if (n == 0) {
                    break;
                }
                if (j + n >= this.length) {
                    return start + 1;
                }
                for (int k = j + 1; k <= j + n; k++) {
                    if (this.chars[k] != '`') {
                        break;
                    } else if (k == j + n) {
                        j = k;
                        break outer;
                    }
                }
            }
            j += 1;
        }
        return j;
    }

    private int skipLink(int start) {
        boolean escape = false;
        int nesting = 0;
        int j = start + 1;
        char c;
        while (j < this.length && (escape || this.chars[j] != ']' || nesting != 0)) {
            c = this.chars[j];
            if (c == '\n' && this.chars[j - 1] == '\n') {
                return start;
            }

            if (escape) {
                escape = false;
            } else {
                escape = c == '\\';
                if (!escape) {
                    if (c == '[') {
                        nesting += 1;
                    } else if (c == ']') {
                        nesting -= 1;
                    }
                }
            }
            j += 1;
        }
        int k = j;
        j += 1;
        boolean extraSpace = false;
        if (j < this.length && Character.isWhitespace(this.chars[j])) {
            j += 1;
            extraSpace = true;
        }
        c = this.chars[j++];
        if (c == '[') {
            while (j < this.length && this.chars[j] != ']') {
                if (this.chars[j] == '\n') {
                    return start;
                }
                j += 1;
            }
        } else if (c == '(' && !extraSpace) {
            while (j < this.length && this.chars[j] != ')' && !isSpace(this.chars[j])) {
                if (this.chars[j] == '\n') {
                    return start;
                }
                j += 1;
            }
            if (j < this.length && this.chars[j] != ')') {
                while (j < this.length && this.chars[j] != ')' && Character.isWhitespace(this.chars[j])) {
                    j += 1;
                }
                if (this.chars[j] == '"') {
                    int quoteStart = j = j + 1;
                    int len = -1;
                    while (j < this.length && this.chars[j] != '\n') {
                        if (this.chars[j] == '"') {
                            len = j - quoteStart;
                        } else if (len > -1) {
                            if (this.chars[j] == ')') {
                                break;
                            } else if (!isSpace(this.chars[j])) {
                                len = -1;
                            }
                        }
                        j += 1;
                    }
                }
                if (this.chars[j] != ')') {
                    return start;
                }
            }
        } else {
            j = k;
        }
        return j;
    }

    private boolean checkLink(char c) {
        return checkLinkInternal(c, this.i + 1, false);
    }

    private boolean checkImage() {
        return checkLinkInternal(this.chars[this.i + 1], this.i + 2,  true);
    }

    private boolean checkLinkInternal(char c, int j, boolean isImage) {
        if (c != '[') {
            return false;
        }
        StringBuffer b = new StringBuffer();
        boolean escape = false;
        boolean space = false;
        int nesting = 0;
        boolean needsEncoding = false;
        while (j < this.length && (escape || this.chars[j] != ']' || nesting != 0)) {
            c = this.chars[j];
            if (c == '\n' && this.chars[j - 1] == '\n') {
                return false;
            }

            if (escape) {
                b.append(c);
                escape = false;
            } else {
                escape = c == '\\';
                if (!escape) {
                    if (c == '[') {
                        nesting += 1;
                    } else if (c == ']') {
                        nesting -= 1;
                    }
                    if (c == '*' || c == '_' || c == '`' || c == '[') {
                        needsEncoding = true;
                    }
                    boolean s = Character.isWhitespace(this.chars[j]);
                    if (!space || !s) {
                        b.append(s ? ' ' : c);
                    }
                    space = s;
                }
            }
            j += 1;
        }
        String text = b.toString();
        b.setLength(0);
        String[] link;
        String linkId;
        int k = j;
        j += 1;
        // this is weird, but we follow the official markup implementation here:
        // only accept space between link text and link target for [][], not for []()
        boolean extraSpace = false;
        if (j < this.length && Character.isWhitespace(this.chars[j])) {
            j += 1;
            extraSpace = true;
        }
        c = this.chars[j++];
        if (c == '[') {
            while (j < this.length && this.chars[j] != ']') {
                if (this.chars[j] == '\n') {
                    return false;
                }
                b.append(this.chars[j]);
                j += 1;
            }
            linkId = b.toString().toLowerCase();
            if (linkId.length() > 0) {
                link = getLink(linkId);
                if (link == null) {
                    return false;
                }
            } else {
                linkId = text.toLowerCase();
                link = getLink(linkId);
                if (link == null) {
                    return false;
                }
            }
        } else if (c == '(' && !extraSpace) {
            link = new String[2];
            while (j < this.length && this.chars[j] != ')' && !isSpace(this.chars[j])) {
                if (this.chars[j] == '\n') {
                    return false;
                }
                b.append(this.chars[j]);
                j += 1;
            }
            link[0] = b.toString();
            if (j < this.length && this.chars[j] != ')') {
                while (j < this.length && this.chars[j] != ')' && Character.isWhitespace(this.chars[j])) {
                    j += 1;
                }
                if (this.chars[j] == '"') {
                    int start = j = j + 1;
                    int len = -1;
                    while (j < this.length && this.chars[j] != '\n') {
                        if (this.chars[j] == '"') {
                            len = j - start;
                        } else if (len > -1) {
                            if (this.chars[j] == ')') {
                                link[1] = new String(this.chars, start, len);
                                break;
                            } else if (!isSpace(this.chars[j])) {
                                len = -1;
                            }
                        }
                        j += 1;
                    }
                }
                if (this.chars[j] != ')') {
                    return false;
                }
            }
        } else {
            j = k;
            linkId = text.toLowerCase();
            link = getLink(linkId);
            if (link == null) {
                return false;
            }
        }
        b.setLength(0);
        if (isImage) {
            this.buffer.append("<img src=\"").append(escapeHtml(link[0])).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
            this.buffer.append(" alt=\"").append(escapeHtml(text)).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
            if (link[1] != null) {
                this.buffer.append(" title=\"").append(escapeHtml(link[1])).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
            }
            this.buffer.append(" />"); //$NON-NLS-1$

        } else {
            this.buffer.append("<a href=\"").append(escapeHtml(link[0])).append("\"");  //$NON-NLS-1$//$NON-NLS-2$
            if (link[1] != null) {
                this.buffer.append(" title=\"").append(escapeHtml(link[1])).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
            }
            this.buffer.append(">"); //$NON-NLS-1$
            if (needsEncoding) {
                MarkdownProcessor wrapped = new MarkdownProcessor();
                this.buffer.append(wrapped.processLinkText(text)).append("</a>"); //$NON-NLS-1$
            } else {
                this.buffer.append(escapeHtml(text)).append("</a>"); //$NON-NLS-1$
            }
        }
        this.i = j + 1;
        return true;
    }

    private boolean checkHtmlLink(char c) {
        if (c != '<') {
            return false;
        }
        int k = this.i + 1;
        int j = k;
        while (j < this.length && !Character.isWhitespace(this.chars[j]) && this.chars[j] != '>') {
            j += 1;
        }
        if (this.chars[j] == '>') {
            String href = new String(this.chars, k, j - k);
            if (href.matches("\\w+:\\S*")) { //$NON-NLS-1$
                String text = href;
                if (href.startsWith("mailto:")) { //$NON-NLS-1$
                    text = href.substring(7);
                    href = escapeMailtoUrl(href);
                }
                this.buffer.append("<a href=\"").append(href).append("\">")  //$NON-NLS-1$//$NON-NLS-2$
                        .append(text).append("</a>"); //$NON-NLS-1$
                this.i = j + 1;
                return true;
            } else if (href.matches("^.+@.+\\.[a-zA-Z]+$")) { //$NON-NLS-1$
                this.buffer.append("<a href=\"") //$NON-NLS-1$
                        .append(escapeMailtoUrl("mailto:" + href)).append("\">") //$NON-NLS-1$ //$NON-NLS-2$
                        .append(href).append("</a>"); //$NON-NLS-1$
                this.i = j + 1;
                return true;
            }
        }
        return false;
    }

    private boolean checkList(char c, int j, int indentation, int blockquoteNesting) {
        int nesting = indentation / 4 + blockquoteNesting;
        if (c >= '0' && c <= '9') {
            while (j < this.length && this.chars[j] >= '0' && this.chars[j] <= '9' ) {
                j += 1;
            }
            if (j < this.length && this.chars[j] == '.') {
                checkCloseList("ol", nesting); //$NON-NLS-1$
                checkOpenList("ol", nesting); //$NON-NLS-1$
                this.i = j + 1;
                return true;
            }
        } else if (c == '*' || c == '+' || c == '-') {
            if (c != '+' && checkHorizontalRule(c, j, nesting)) {
                return true;
            }
            j += 1;
            if (j < this.length && isSpace(this.chars[j])) {
                checkCloseList("ul", nesting); //$NON-NLS-1$
                checkOpenList("ul", nesting); //$NON-NLS-1$
                this.i = j;
                return true;
            }
        }
        if (isParagraphStart()) {
            // never close list unless there's an empty line
            checkCloseList(null, nesting - 1);
        }
        return false;
    }

    private void checkOpenList(String tag, int nesting) {
        Element list = this.stack.search(ListElement.class);
        if (list == null || !tag.equals(list.tag) || nesting != list.nesting) {
            list = new ListElement(tag, nesting);
            this.stack.push(list);
            list.open();
        } else {
            this.stack.closeElementsExclusive(list);
            this.buffer.insert(getBufferEnd(), "</li>"); //$NON-NLS-1$
        }
        openTag("li", this.buffer); //$NON-NLS-1$
        this.listParagraphs = isParagraphStart();
        this.lineMarker = this.paragraphStartMarker = this.buffer.length();
        this.state = MarkdownProcessor.LIST;
    }

    private void checkCloseList(String tag, int nesting) {
        Element elem = this.stack.search(ListElement.class);
        while (elem != null &&
                (elem.nesting > nesting ||
                (elem.nesting == nesting && tag != null && !elem.tag.equals(tag)))) {
            this.stack.closeElements(elem);
            elem = this.stack.peekNestedElement();
            this.lineMarker = this.paragraphStartMarker = this.buffer.length();
        }
    }

    private boolean checkCodeBlock(char c, int j, int indentation, int blockquoteNesting) {
        int nesting = indentation / 4;
        int nestedLists = this.stack.countNestedLists(null);
        Element code;
        if (nesting - nestedLists <= 0) {
            code = this.stack.findNestedElement(CodeElement.class, blockquoteNesting + nestedLists);
            if (code != null) {
                this.stack.closeElements(code);
                this.lineMarker = this.paragraphStartMarker = this.buffer.length();
            }
            return false;
        }
        code = this.stack.isEmpty() ? null : (Element) this.stack.peek();
        if (!(code instanceof CodeElement)) {
            code = new CodeElement(blockquoteNesting + nestedLists);
            code.open();
            this.stack.push(code);
        }
        int sub = 4 + nestedLists * 4;
        for (int k = sub; k < indentation; k++) {
            this.buffer.append(' ');
        }
        while(j < this.length && this.chars[j] != '\n') {
            if (this.chars[j] == '&') {
                this.buffer.append("&amp;"); //$NON-NLS-1$
            } else if (this.chars[j] == '<') {
                this.buffer.append("&lt;"); //$NON-NLS-1$
            } else if (this.chars[j] == '>') {
                this.buffer.append("&gt;"); //$NON-NLS-1$
            } else if (this.chars[j] == '\t') {
                this.buffer.append("   "); //$NON-NLS-1$
            } else {
                this.buffer.append(this.chars[j]);
            }
            j += 1;
        }
        this.codeEndMarker = this.buffer.length();
        this.i = j;
        this.state = MarkdownProcessor.CODE;
        return true;
    }

    private boolean checkBlockquote(char c, int j, int indentation, int blockquoteNesting) {
        int nesting = indentation / 4;
        Element elem = this.stack.findNestedElement(BlockquoteElement.class, nesting + blockquoteNesting);
        if (c != '>' && isParagraphStart() || nesting > this.stack.countNestedLists(elem)) {
            elem = this.stack.findNestedElement(BlockquoteElement.class, blockquoteNesting);
            if (elem != null) {
                this.stack.closeElements(elem);
                this.lineMarker = this.paragraphStartMarker = this.buffer.length();
            }
            return false;
        }
        nesting +=  blockquoteNesting;
        elem = this.stack.findNestedElement(BlockquoteElement.class, nesting);
        if (c == '>') {
            this.stack.closeElementsUnlessExists(BlockquoteElement.class, nesting);
            if (elem != null && !(elem instanceof BlockquoteElement)) {
                this.stack.closeElements(elem);
                elem = null;
            }
            if (elem == null || elem.nesting < nesting) {
                elem = new BlockquoteElement(nesting);
                elem.open();
                this.stack.push(elem);
                this.lineMarker = this.paragraphStartMarker = this.buffer.length();
            } else {
                this.lineMarker = this.buffer.length();
            }
            this.i = isSpace(this.chars[j+ 1]) ? j + 2 : j + 1;
            this.state = MarkdownProcessor.NEWLINE;
            checkBlock(nesting + 1);
            return true;
        }
        return elem instanceof BlockquoteElement;
    }


    private void checkParagraph(boolean paragraphs) {
        int paragraphEndMarker = getBufferEnd();
        if (paragraphs && paragraphEndMarker > this.paragraphStartMarker &&
                (this.chars[this.i + 1] == '\n' || this.buffer.charAt(this.buffer.length() - 1) == '\n')) {
            this.buffer.insert(paragraphEndMarker, "</p>"); //$NON-NLS-1$
            this.buffer.insert(this.paragraphStartMarker, "<p>"); //$NON-NLS-1$
        } else if (this.i > 1 && this.chars[this.i-1] == ' ' && this.chars[this.i-2] == ' ') {
            this.buffer.append("<br />"); //$NON-NLS-1$
        }
    }

    private boolean checkAtxHeader(char c, int j) {
        if (c == '#') {
            int nesting = 1;
            int k = j + 1;
            while (k < this.length && this.chars[k++] == '#') {
                nesting += 1;
            }
            HeaderElement header = new HeaderElement(nesting);
            header.open();
            this.stack.push(header);
            this.state = MarkdownProcessor.HEADER;
            this.i = k - 1;
            return true;
        }
        return false;
    }

    private boolean checkHtmlBlock(char c, int j) {
        if (c == '<') {
            j += 1;
            int k = j;
            while (k < this.length && Character.isLetterOrDigit(this.chars[k])) {
                k += 1;
            }
            String tag = new String(this.chars, j, k - j).toLowerCase();
            if (blockTags.contains(tag)) {
                this.state = MarkdownProcessor.HTML_BLOCK;
                return true;
            }
        }
        return false;
    }

    private void checkHeader() {
        char c = this.chars[this.i + 1];
        if (c == '-' || c == '=') {
            int j = this.i + 1;
            while (j < this.length && this.chars[j] == c) {
                j++;
            }
            if (j < this.length && this.chars[j] == '\n') {
                if (c == '=') {
                    this.buffer.insert(this.lineMarker, "<h1>"); //$NON-NLS-1$
                    this.buffer.append("</h1>"); //$NON-NLS-1$
                } else {
                    this.buffer.insert(this.lineMarker, "<h2>"); //$NON-NLS-1$
                    this.buffer.append("</h2>"); //$NON-NLS-1$
                }
                this.i = j;
            }
        }
    }

    private boolean checkHorizontalRule(char c, int j, int nesting) {
        if (c != '*' && c != '-') {
            return false;
        }
        int count = 1;
        int k = j;
        while (k < this.length && (isSpace(this.chars[k]) || this.chars[k] == c)) {
            k += 1;
            if (this.chars[k] == c) {
                 count += 1;
            }
        }
        if (count >= 3 &&  this.chars[k] == '\n') {
            checkCloseList(null, nesting - 1);
            this.buffer.append("<hr />"); //$NON-NLS-1$
            this.i = k;
            return true;
        }
        return false;
    }

    private void cleanup() {
        this.links = null;
        this.chars = null;
        this.buffer = null;
        this.stack = null;
    }

    private String escapeHtml(String str) {
        if (str.indexOf('"') > -1) {
            str = str.replace("\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (str.indexOf('<') > -1) {
            str = str.replace("\"", "&lt;");  //$NON-NLS-1$//$NON-NLS-2$
        }
            if (str.indexOf('>') > -1) {
            str = str.replace("\"", "&gt;");  //$NON-NLS-1$//$NON-NLS-2$
        }
        return str;
    }

    private String escapeMailtoUrl(String str) {
        StringBuffer b = new StringBuffer();
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            double random = Math.random();
            if (random < 0.5) {
                b.append("&#x").append(Integer.toString(chars[i], 16)).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (random < 0.9) {
                b.append("&#").append(Integer.toString(chars[i], 10)).append(";");  //$NON-NLS-1$//$NON-NLS-2$
            } else {
                b.append(chars[i]);
            }
        }
        return b.toString();
    }

    private synchronized String processLinkText() {
        this.buffer = new StringBuilder((int) (this.length * 1.2));
        this.line = 1;
        boolean escape = false;

        for (this.i = 0; this.i < this.length; ) {
            char c = this.chars[this.i];

            if (escape) {
                this.buffer.append(c);
                escape = false;
                this.i += 1;
                continue;
            } else if (c == '\\') {
                escape = true;
                this.i += 1;
                continue;
            }
            switch (c) {
                case '*':
                case '_':
                    if (checkEmphasis(c)) {
                        continue;
                    }
                    break;

                case '`':
                    if (checkCodeSpan(c)) {
                        continue;
                    }
                    break;

                case '!':
                    if (checkImage()) {
                        continue;
                    }
                    break;
            }

            this.buffer.append(c);
            this.i += 1;
        }
        return this.buffer.toString().trim();
    }

    boolean isLinkQuote(char c) {
        return c == '"' || c == '\'' || c == '(';
    }

    boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    boolean isParagraphStart() {
        return this.paragraphStartMarker == this.lineMarker;
    }

    int getBufferEnd() {
        int l = this.buffer.length();
        while(l > 0 && this.buffer.charAt(l - 1) == '\n') {
            l -= 1;
        }
        return l;
    }

    class ElementStack extends Stack {
        private static final long serialVersionUID = 8514510754511119691L;

        private Element search(Class clazz) {
            for (int i = size() - 1; i >= 0; i--) {
                Element elem = (Element) get(i);
                if (clazz.isInstance(elem)) {
                    return elem;
                }
            }
            return null;
        }

        private int countNestedLists(Element startFromElement) {
            int count = 0;
            for (int i = size() - 1; i >= 0; i--) {
                Element elem = (Element) get(i);
                if (startFromElement != null) {
                    if (startFromElement == elem) {
                        startFromElement = null;
                    }
                    continue;
                }
                if (elem instanceof ListElement) {
                    count += 1;
                } else if (elem instanceof BlockquoteElement) {
                    break;
                }
            }
            return count;
        }

        private Element peekNestedElement() {
            for (int i = size() - 1; i >= 0; i--) {
                Element elem = (Element) get(i);
                if (elem instanceof ListElement || elem instanceof BlockquoteElement) {
                    return elem;
                }
            }
            return null;
        }

        private Element findNestedElement(Class type, int nesting) {
            for (Iterator it = iterator(); it.hasNext();) {
                Element elem = (Element) it.next();
                if (nesting == elem.nesting && type.isInstance(elem)) {
                    return elem;
                }
            }
            return null;
        }

        private void closeElements(Element element) {
            do {
                ((Element) peek()).close();
            } while (pop() != element);
        }

        private void closeElementsExclusive(Element element) {
            while(peek() != element) {
                ((Element) pop()).close();
            }
        }

        private void closeElementsUnlessExists(Class type, int nesting) {
            Element elem = this.findNestedElement(type, nesting);
            if (elem == null) {
                while(MarkdownProcessor.this.stack.size() > 0) {
                    elem = (Element) this.peek();
                    if (elem != null && elem.nesting >= nesting) {
                        ((Element) MarkdownProcessor.this.stack.pop()).close();
                    } else {
                        break;
                    }
                }
            }
        }
    }

    class Element {
        String tag;
        int nesting, mod;

        void open() {
            openTag(this.tag, MarkdownProcessor.this.buffer);
        }

        void close() {
            MarkdownProcessor.this.buffer.insert(getBufferEnd(), "</" + this.tag + ">"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    class BaseElement extends Element {
        @Override
        void open() {}
        @Override
        void close() {}
    }

    class BlockquoteElement extends Element {
        BlockquoteElement(int nesting) {
            this.tag = "blockquote"; //$NON-NLS-1$
            this.nesting = nesting;
        }
    }

    class CodeElement extends Element {
        CodeElement(int nesting) {
            this.nesting = nesting;
        }

        @Override
        void open() {
            openTag("pre", MarkdownProcessor.this.buffer); //$NON-NLS-1$
            openTag("code", MarkdownProcessor.this.buffer); //$NON-NLS-1$
        }

        @Override
        void close() {
            MarkdownProcessor.this.buffer.insert(MarkdownProcessor.this.codeEndMarker, "</code></pre>"); //$NON-NLS-1$
        }
    }

    class HeaderElement extends Element {
        HeaderElement(int nesting) {
            this.nesting = nesting;
            this.tag = "h" + nesting; //$NON-NLS-1$
        }
    }

    class ListElement extends Element {
        ListElement(String tag, int nesting) {
            this.tag = tag;
            this.nesting = nesting;
        }

        @Override
        void close() {
            MarkdownProcessor.this.buffer.insert(getBufferEnd(), "</li></" + this.tag + ">");        }  //$NON-NLS-1$//$NON-NLS-2$
    }

    class Emphasis extends Element {
        int end;
        Emphasis(int mod, int end) {
            this.mod = mod;
            this.end = end;
            this.tag = mod == 1 ? "em" : "strong";  //$NON-NLS-1$//$NON-NLS-2$
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println(Messages.getString("MarkdownProcessor.0")); //$NON-NLS-1$
            return;
        }
        MarkdownProcessor processor = new MarkdownProcessor(new File(args[0]));
        System.out.println(processor.process());
    }


}

