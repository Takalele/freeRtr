package net.freertr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgInit;
import net.freertr.pipe.pipeShell;
import net.freertr.serv.servOpenflow;
import net.freertr.serv.servP4lang;
import net.freertr.user.userHelping;

/**
 * version utils
 *
 * @author matecsaba
 */
public class version {

    private version() {
    }

    /**
     * 9.1.1
     */
    public final static String verNum = verCore.year + "." + verCore.month + "." + verCore.day;

    /**
     * v9.1.1-rel
     */
    public final static String VerNam = "v" + verNum + verCore.state;

    /**
     * ros v9.1.1-rel
     */
    public final static String namVer = verCore.name + " " + VerNam;

    /**
     * ros/9.1.1-rel
     */
    public final static String usrAgnt = verCore.name + "/" + verNum + verCore.state;

    /**
     * ros v9.1.1-rel, done by me.
     */
    public final static String headLine = namVer + ", done by " + verCore.author + ".";

    /**
     * mimetype text
     */
    public final static String[] mimetypes = {
        // text
        "html   text/html",
        "htm    text/html",
        "css    text/css",
        "rtf    text/richtext",
        "text   text/plain",
        "txt    text/plain",
        "csv    text/csv",
        "md     text/markdown",
        "*      text/plain",
        // image
        "webp   image/webp",
        "gif    image/gif",
        "jpeg   image/jpeg",
        "jpg    image/jpeg",
        "tiff   image/tiff",
        "tif    image/tiff",
        "bmp    image/bmp",
        "png    image/png",
        "svg    image/svg+xml",
        "ico    image/x-icon",
        "pbm    image/x-portable-bitmap",
        "pgm    image/x-portable-graymap",
        "pnm    image/x-portable-anymap",
        "ppm    image/x-portable-pixmap",
        "xbm    image/x-xbitmap",
        "xpm    image/x-xpixmap",
        // video
        "webm   video/webm",
        "mjpeg  video/x-motion-jpeg",
        "avi    video/msvideo",
        "mov    video/quicktime",
        "qt     video/quicktime",
        "mpeg   video/mpeg",
        "mpg    video/mpeg",
        "mp4    video/mp4",
        "mkv    video/x-matroska",
        "3gp    video/3gpp",
        "3g2    video/3gpp2",
        "ogv    video/ogg",
        // audio
        "weba   audio/weba",
        "aif    audio/x-aiff",
        "aiff   audio/x-aiff",
        "wav    audio/wav",
        "midi   audio/midi",
        "mid    audio/midi",
        "rmi    audio/midi",
        "ram    audio/x-pn-realaudio",
        "rpm    audio/x-pn-realaudio-plugin",
        "ra     audio/x-realaudio",
        "rm     audio/x-pn-realaudio",
        "mp3    audio/mpeg",
        "oga    audio/ogg",
        "flac   audio/flac",
        "aac    audio/aac",
        // application
        "bin    application/octet-stream",
        "jar    application/java-archive",
        "doc    application/msword",
        "docx   application/msword",
        "dvi    application/x-dvi",
        "eps    application/postscript",
        "ps     application/postscript",
        "gz     application/x-gzip",
        "bz2    application/x-bzip2",
        "js     application/javascript",
        "latex  application/x-latex",
        "lzh    application/x-lzh",
        "pdf    application/pdf",
        "epub   application/epub+zip",
        "swf    application/x-shockwave-flash",
        "tar    application/tar",
        "tcl    application/x-tcl",
        "tex    application/x-tex",
        "tgz    application/x-gzip",
        "zip    application/zip",
        "xml    application/xml",
        "ogg    application/ogg",
        // wireless application
        "wml    text/vnd.wap.wml",
        "wbmp   image/vnd.wap.wbmp"
    };

    /**
     * get show logo text
     *
     * @param head needed extra lines
     * @return list
     */
    public static List<String> shLogo(int head) {
        List<String> sa = new ArrayList<String>();
        if ((head & 0x01) != 0) {
            sa.add("");
        }
        if ((head & 0x02) != 0) {
            sa.add(headLine);
        }
        if ((head & 0x04) != 0) {
            sa.add("");
        }
        if ((head & 0x08) != 0) {
            sa.addAll(Arrays.asList(verCore.logo));
        }
        if ((head & 0x10) != 0) {
            sa.add("");
        }
        if ((head & 0x20) != 0) {
            sa.add(headLine);
        }
        if ((head & 0x40) != 0) {
            sa.add("");
        }
        if ((head & 0x80) != 0) {
            sa.addAll(Arrays.asList(verCore.license));
        }
        if ((head & 0x100) != 0) {
            sa.add("");
        }
        if ((head & 0x200) != 0) {
            sa.add(verNum);
        }
        if ((head & 0x400) != 0) {
            sa.add(bits.time2str(cfgAll.timeZoneName, getFileDate(), 3));
        }
        if ((head & 0x800) != 0) {
            sa.add(bits.time2str(cfgAll.timeZoneName, getFileDate(), 4));
        }
        if ((head & 0x1000) != 0) {
            sa.add(usrAgnt);
        }
        if ((head & 0x2000) != 0) {
            sa.add(verCore.homeUrl1);
        }
        return sa;
    }

    /**
     * generate help list
     *
     * @param hl help to update
     * @param beg beginning
     */
    public static void genSecHelp(userHelping hl, int beg) {
        List<String> l = getSecList();
        for (int i = 0; i < l.size(); i++) {
            String a = l.get(i);
            hl.add(null, beg + " .  " + a + "   sh0w m30www s0m30www " + a);
        }
    }

    /**
     * find in secret list
     *
     * @param a string to find
     * @return found, -1 if nothing
     */
    public static int findSecret(String a) {
        List<String> lst = getSecList();
        for (int i = 0; i < lst.size(); i++) {
            String s = lst.get(i);
            if (s.equals(a)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * get show secret text
     *
     * @param typ type of secret
     * @return list
     */
    public static List<String> shSecret(int typ) {
        ArrayList<String> l = new ArrayList<String>();
        switch (typ) {
            case 0:
                l.add("");
                l.add(" ... g00 l3n4 g3nZZZ 1T 0.000v ...");
                break;
            case 1:
                l.add("");
                l.add("   /~~!~\\");
                l.add("  |      |_______");
                l.add("  | |<3Y |       |");
                l.add("   \\____/        |");
                l.add("");
                break;
            case 2:
                l.add("");
                l.add("   /~~\\   /~~\\");
                l.add("  |    \\_/    |");
                l.add("   \\         /");
                l.add("    \\  L0v3 /");
                l.add("     \\     /");
                l.add("      \\   /");
                l.add("       \\ /");
                l.add("        V");
                l.add("");
                break;
            case 3:
                l.add("");
                l.add("                 \\   /");
                l.add("                 .\\-/.");
                l.add("             /\\  @o o@  /\\");
                l.add("            /  \\ /~-~\\ /  \\");
                l.add("                y  Y  V");
                l.add("          ,-^-./   |   \\,-^-.");
                l.add("         /    {   BuG   }    \\");
                l.add("               \\   |   /");
                l.add("               /\\  A  /\\");
                l.add("              /  \\/ \\/  \\");
                l.add("             /           \\");
                l.add("");
                break;
            case 4:
                l.add("d3ar c0d3 r3v13w3r$ZZZ:");
                l.add("l3mm3 mansplain a b1t");
                l.add("s0 pl3a$3 d0n't g3t 1t a way t33 s3r1ously || p3r(f)S0nallY...");
                l.add("once we'll see, and i do regular refactorings as the world changes");
                l.add("at the moment i'm fine with my integration and interop tests against the big vendors");
                l.add("that usually ends in 7-8 minutes on a random xeon with graal-vm-community");
                l.add("i'm an networker and just hobby programmer and not a vegetable dealer nor anything");
                l.add("so dear reviewers team sorry 4 hurtin' your eyes tonight:)");
                l.add("my code smells and you'll grep some shit out like _and_ in the sources");
                l.add("but it's a way crazier if you do the same to the linux kernel like _and_ and so");
                l.add("so long-live creativity! ;)");
                l.add(verCore.author);
                break;
            case 5:
                l.add("                   \\ /");
                l.add("                 -->*<--");
                l.add("                   /o\\");
                l.add("                  /_\\_\\");
                l.add("                 /_/_0_\\");
                l.add("                /_o_\\_\\_\\");
                l.add("               /_/_/_/_/o\\");
                l.add("              /@\\_\\_\\@\\_\\_\\");
                l.add("             /_/_/O/_/_/_/_\\");
                l.add("            /_\\_\\_\\_\\_\\o\\_\\_\\");
                l.add("           /_/0/_/_/_0_/_/@/_\\");
                l.add("          /_\\_\\_\\_\\_\\_\\_\\_\\_\\_\\");
                l.add("         /_/o/_/_/@/_/_/o/_/0/_\\");
                l.add("            [___]");
                l.add("b3ar pspsps sh0w m0r3 r3sp3ct y0urs3lv3s");
                break;
            case 6:
                l.add("      /OO\\");
                l.add("      /||\\");
                l.add("       ||");
                l.add("       ||");
                l.add("       ||");
                l.add("       ||");
                l.add("       ||");
                l.add("       ||");
                l.add("      0||@");
                break;
            case 7:
                l.add("    _      _      _");
                l.add("  >(0)__ <(0)__ =(0)__");
                l.add("   (___/  (___/  (___/");
                break;
            case 8:
                l.add("   |~~~~~~~~|");
                l.add("   | |_@@_| |");
                l.add("   |   __   |");
                l.add("   |  (00)  |");
                l.add("   |        |");
                l.add("   |________|");
                break;
            case 9:
                l.add("                         c@t5eTAN|<");
                l.add("                                                     _..----.._  ");
                l.add("                                                    ]_.--._____[  ");
                l.add("                                                  ___|'--'__..|--._   ");
                l.add("                              __               \"\"\"    ;            :  ");
                l.add("                            ()_ \"\"\"\"---...__.'\"\"!\":  /    ___       :  ");
                l.add("                               \"\"\"---...__\\]..__] | /    [ 0 ]      :  ");
                l.add("                                          \"\"\"!--./ /      \"\"\"        :  ");
                l.add("                                   __  ...._____;\"\"'.__________..--..:_  ");
                l.add("                                  /  !\"''''''!''''''''''|''''/' ' ' ' \\\"--..__  __..  ");
                l.add("                                 /  /.--.    |          |  .'          \\' ' '.\"\"--.{'.  ");
                l.add("             _...__            >=7 //.-.:    |          |.'             \\ ._.__  ' '\"\"'. ");
                l.add("          .-' /    \"\"\"\"----..../ \"\">==7-.....:______    |                \\| |  \"\";.;-\"> \\  ");
                l.add("          \"\"\"\";           __..\"   .--\"/\"\"\"\"\"----....\"\"\"\"\"----.....H_______\\_!....'----\"\"\"\"]  ");
                l.add("        _..---|._ __..--\"\"       _!.-=_.            \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"                   ;\"\"\"  ");
                l.add("       /   .-\";-.'--...___     .\" .-\"\"; ';\"\"-\"\"-...^..__...-v.^___,  ,__v.__..--^\"--\"\"-v.^v,  ");
                l.add("      ;   ;   |'.         \"\"\"-/ ./;  ;   ;\\P.        ;   ;        \"\"\"\"____;  ;.--\"\"\"\"// '\"\"<,");
                l.add("      ;   ;   | 1            ;  ;  '.: .'  ;<   ___.-'._.'------\"\"\"\"\"\"____'..'.--\"\"\";;'  o ';  ");
                l.add("      '.   \\__:/__           ;  ;--\"\"()_   ;'  /___ .-\" ____---\"\"\"\"\"\"\" __.._ __._   '>.,  ,/;  ");
                l.add("        \\   \\    /\"\"\"<--...__;  '_.-'/; \"\"; ;.'.'  \"-..'    \"-.      /\"/    `__. '.   \"---\"; ");
                l.add("         '.  'v ; ;     ;;    \\  \\ .'  \\ ; // /    _.-\" \"-._   ;    : ;   .-'__ '. ;   .^\".'      ");
                l.add("           '.  '; '.   .'/     '. `-.__.' /;;;   .o__.---.__o. ;    : ;   '\"\";;\"\"' ;v^\" .^ ");
                l.add("             '-. '-.___.'<__v.^,v'.  '-.-' ;|:   '    :      ` ;v^v^'.'.    .;'.__/_..-'   ");
                l.add("                '-...__.___...---\"\"'-.   '-'.;\\     'WW\\     .'_____..>.\"^\"-\"\"\"\"\"\"\"\" ");
                l.add("                                      '--..__ '\"._..'  '\"-;;\"\"\" ");
                l.add("                                             \"\"\"---'\"\"\"\"\"\"  ");
                l.add("");
                break;
            case 10:
                l.add("      .____.");
                l.add("   xuu$``$$$uuu.");
                l.add(" . $``$  $$$`$$$");
                l.add("dP*$  $  $$$ $$$");
                l.add("?k $  $  $$$ $$$");
                l.add(" $ $  $  $$$ $$$");
                l.add(" \":$  $  $$$ $$$");
                l.add("  N$  $  $$$ $$$");
                l.add("  $$  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $$#$  $$$ $$$");
                l.add("   $$'$  $$$ $$$");
                l.add("   $$`R  $$$ $$$");
                l.add("   $$$&  $$$ $$$");
                l.add("   $#*$  $$$ $$$");
                l.add("   $  $  $$$ @$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $$$ $$$");
                l.add("   $  $  $B$ $$&.");
                l.add("   $  $  $D$ $$$$$muL.");
                l.add("   $  $  $Q$ $$$$$  `\"**mu..");
                l.add("   $  $  $R$ $$$$$    k  `$$*t");
                l.add("   $  @  $$$ $$$$$    k   $$!4");
                l.add("   $ x$uu@B8u$NB@$uuuu6...$$X?");
                l.add("   $ $(`RF`$`````R$ $$5`\"\"\"#\"R");
                l.add("   $ $\" M$ $     $$ $$$      ?");
                l.add("   $ $  ?$ $     T$ $$$      $");
                l.add("   $ $F H$ $     M$ $$K      $  ..");
                l.add("   $ $L $$ $     $$ $$R.     \"d$$$$Ns.");
                l.add("   $ $~ $$ $     N$ $$X      .\"    \"02h");
                l.add("   $ 4k f  $     *$ $$&      R       \"iN");
                l.add("   $ $$ %uz!     tuuR$$:     Buu      ?`:");
                l.add("   $ $F          $??$8B      | '*Ned*$~L$");
                l.add("   $ $k          $'@$$$      |$.suu+!' !$");
                l.add("   $ ?N          $'$$@$      $*`      d:\"");
                l.add("   $ dL..........M.$&$$      5       d\"P");
                l.add(" ..$.^\"*I$RR*$C\"\"??77*?      \"nu...n*L*");
                l.add("'$C\"R   ``\"\"!$*@#\"\"` .uor    bu8BUU+!`");
                l.add("'*@m@.       *d\"     *$Rouxxd\"```$");
                l.add("     R*@mu.           \"#$R *$    !");
                l.add("     *%x. \"*L               $     %.");
                l.add("        \"N  `%.      ...u.d!` ..ue$$$o..");
                l.add("         @    \".    $*\"\"\"\" .u$$$$$$$$$$$$beu...");
                l.add("        8  .mL %  :R`     x$$$$$$$$$$$$$$$$$$$$$$$$$$WmeemeeWc");
                l.add("       |$e!\" \"s:k 4      d$N\"`\"#$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("       $$      \"N @      $?$    F$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("       $@       ^%Uu..   R#8buu$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                  ```\"\"*u$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                         #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                          \"5$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                            `*$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                              ^#$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                                 \"*$$$$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                                   `\"*$$$$$$$$$$$$$$$$$$$$$$$>");
                l.add("                                       ^!$$$$$$$$$$$$$$$$$$$$>");
                l.add("                                           `\"#+$$$$$$$$$$$$$$>");
                l.add("                                                 \"\"**$$$$$$$$>");
                l.add("                                                        ```\"\"");
                break;
            case 11:
                l.add(" r1ffl3 ");
                l.add(" ,________________________________   ");
                l.add("|__________,----------._ [____]  \"\"-,__  __...-----===\"");
                l.add("        (_(||||@@@@||||)___________/   \"\"             |");
                l.add("           `----------' Kr0gg@@[ ))\"-,                |");
                l.add("                                \"\"    `,  _,--...___  |");
                l.add("                                        `/          \"\"\"");
                break;
            case 12:
                l.add(" kn1f3 ");
                l.add("       .---.");
                l.add("       |---|");
                l.add("       |---|");
                l.add("       |---|");
                l.add("   .---^ - ^---.");
                l.add("   :___________:");
                l.add("      | 0|//|");
                l.add("      |0 |//|");
                l.add("      |@@|//|");
                l.add("      |@@|//|");
                l.add("      |@@|//|");
                l.add("      |0|//|");
                l.add("      |0 |.-|");
                l.add("      |0-'@@|");
                l.add("       \\@@/");
                l.add("        \\@/");
                l.add("         V");
                break;
            case 13:
                l.add("     ... th3 n3w m30wcr0n var1ant ...");
                l.add("                                        XX ");
                l.add("     XX   XX    XX                    XXXXX ");
                l.add("     XX   XX   XXX                   XX   XX");
                l.add("     XX   XX  XXXX                   XX    X");
                l.add("     XX   XX    XX   XX XXX  XX  XX  XX     ");
                l.add("     XX   XX    XX    XXX XX XX  XX   XXXXX ");
                l.add("     XX   XX    XX    XX  XX XX  XX       XX");
                l.add("     XX   XX    XX    XX     XX  XX       XX");
                l.add("      XX XX     XX    XX     XX  XX  X    XX");
                l.add("       XXX      XX    XX     XX  XX  XX   XX");
                l.add("        X     XXXXXX XXXX     XXX XX  XXXXX ");
                l.add("                                        XX ");
                l.add("");
                break;
            case 14:
                l.add("         ... TAN< u N0T ...");
                l.add("            .....        ...       ");
                l.add("     ::::::::::::::_____::::::::.");
                l.add("    .::::::::::::::::::::::::::::::::::::::::::::::::::. ");
                l.add(":::::::::::::::::::::::::::::::::::::::::::::::::::::::...");
                l.add(" :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
                l.add(" :::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
                l.add(" :::::::::::::@:::::::::::::::::::::::::");
                l.add(" ::::::::::::@:@::::::::::::::::::::::::::::::::::");
                l.add("::::::::::::::@:::::::::::::::::::::::::::");
                l.add(" :::::::::::::X:::::::::::::::::::::::::: ");
                l.add(" .:::::::::::::0XXX::::::::::::::::::::'");
                l.add("::::::::::::::::::::::::::::::::");
                l.add("`::::::::::::::::::::::::::::::::::::::::");
                l.add(" '::::::::::::::::::::   ^^^^");
                l.add("          :::::::::::::");
                l.add("          :::::::::::::");
                l.add("          :::::::::::::::");
                l.add("          :::::::::::::::::");
                l.add("          ::::::::::::::::::.");
                l.add("         :::::::::::::::'");
                l.add("        :::::::::::::'");
                l.add("       ::::::::::'");
                l.add("");
                break;
            case 15:
                l.add(" d0 n0t b3ar w1th a m30www!");
                l.add("                  _         _");
                l.add(" .-\"\"-.          ( )-\"```\"-( )          .-\"\"-.");
                l.add("/ O O  \\          /         \\          /  O O \\");
                l.add("|O .-.  \\        /   0 _ 0   \\        /  .-. O|");
                l.add("\\ (   )  '.    _|     (_)     |     .'  (   ) /");
                l.add(" '.`-'     '-./ |             |`\\.-'     '-'.'");
                l.add("   \\         |  \\   \\     /   /  |         /");
                l.add("    \\        \\   '.  '._.'  .'   /        /");
                l.add("     \\        '.   `'-----'`   .'        /");
                l.add("      \\   .'    '-._        .-'\\   '.   /");
                l.add("       |/`          `'''''')    )    `\\|");
                l.add("       /                  (    (      ,\\");
                l.add("      ;                    \\    '-..-'/ ;");
                l.add("      |                     '.       /  |");
                l.add("      |                       `'---'`   |");
                l.add("      ;                                 ;");
                l.add("       \\                               /");
                l.add("        `.                           .'");
                l.add("          '-._                   _.-'");
                l.add("          __/`\"  '  - - -  ' \"`` \\__");
                l.add("         /`            /^\\           `\\");
                l.add("         \\(          .'   '.         )/");
                l.add("          '.(__(__.-'       '.__)__).'");
                l.add("");
                break;
            case 16:
                l.add("                    @@    00    00 ");
                l.add("              0000  @@000000@@00@@XXOO@@ ");
                l.add("          @@  OO0000@@000000000000@@@@OO ");
                l.add("          OO0000@@OOXXOO  @@  XXOOOO@@000000  ");
                l.add("      OO    00@@00XX  00  @@  OOXX  @@@@@@00  ");
                l.add("      0000@@00    00  @@  00  00  00XX  000000");
                l.add("        0000XX00  00  00  @@  @@  00  00OO@@00@@  ");
                l.add("    @@OO@@@@  XX@@  @@XXOO@@OO@@00XX00OO  XX@@@@O ");
                l.add("    OO0000XX00XXXX@@OOOO00@@00OO0000OOXX0000XX00@@0");
                l.add("      @@00    0000XX00000000000000OO0000XX    0000 ");
                l.add("  00000000@@00OOXX@@00@@@@00@@@@00@@XXXX00@@@@OO0000@");
                l.add("    OO00XX    OO00@@@@00XX  XX00@@00@@OOXX      @@00XX0");
                l.add("    00@@@@@@@@@@@@@@@@@@      0000@@@@@@@@@@@@@@@@00OO00");
                l.add("XX@@00@@        OO00@@00      000000OOXX        @@@@@@");
                l.add("      00OOOO@@00XXOO@@00000000000000XXOO@@00OOXX00XX  ");
                l.add("    000000XX  OO@@OO@@00@@00@@@@00OO00OO    XX@@00@@XX ");
                l.add("  0000@@00  @@OO  @@OO0000@@00OOOO00XXOO00OO  00@@00@@ ");
                l.add("      OO00@@    @@XX@@XX00@@00OO@@  @@XX  00@@0000 ");
                l.add("    XX00@@@@  @@XXOOOOOOXX@@  00XX00  00XX0000@@00OO");
                l.add("    XXXX  0000XX  @@  @@  @@  @@  00XX  0000@@    OO");
                l.add("        OO@@000000XX  00  @@  00XX  @@00@@00@@OO ");
                l.add("        @@  OO0000@@OOOO  @@  XX0000000000  XX@@ ");
                l.add("            0000@@00@@00@@@@@@@@@@00@@@@@@");
                l.add("            00XX0000@@@@@@00@@@@00@@OO  @@");
                l.add("        XX  000000@@@@0000  0000  00OO  ");
                break;
            case 17:
                l.add("... g0d sav3 th3 qu33n --- http://hacknasa.mp.ls/ ...");
                l.add("                                    00@@@@@@@@@@@@~~~");
                l.add("        88888b.  8888b. .d8888 @@8008b.");
                l.add("        888  88b     88 88K@@@@ 00   88b ");
                l.add("        888  888.d888888 Y888800.d888888 ");
                l.add("        888@@888888  888    0088 88  888 ");
                l.add("...@@@@@888  888 Y888888 88888P' Y888888 ");
                l.add("                        00");
                l.add("                      00");
                l.add("                    00");
                l.add("                  00");
                l.add("                00");
                l.add("              00");
                l.add("            00");
                l.add("          00");
                l.add("        00");
                l.add("      00");
                l.add("    00");
                l.add("  00");
                l.add("00");
                l.add("~");
                break;
            case 18:
                l.add(" http://c3rn.ch http://sw1t.ch");
                l.add("");
                l.add(" XXXXXXXXXXX   OO");
                l.add("XX         XX  OO");
                l.add("XX     @@@@@@@@@");
                l.add("XX    OO   XX  OO");
                l.add("XX    OO   XX  OO");
                l.add("XX    OO   XX  OO");
                l.add(" XXXXXOOXXXX@  OO");
                l.add(" @    OO     @ OO");
                l.add("  @   OO      @OO");
                l.add("     @ @@@@@@@@@");
                l.add("    @           @");
                break;
            case 19:
                l.add("----|------------|-----------|----");
                l.add("    |        --/ - \\--       |");
                l.add("   -|---------|  @  |--------|-");
                l.add("              /\\ _ /\\");
                l.add("           []/       \\[]");
                break;
            case 20:
                l.add(" us3d c0tt@n");
                l.add("      _   _");
                l.add("     ( `O' )");
                l.add("      ) \0 (");
                l.add("      |`@'|");
                l.add("      }-  {");
                l.add("      }-- {");
                l.add("      } - {");
                l.add("      |   |");
                l.add("      |___|");
                l.add("     (_____)");
                l.add("");
                break;
            case 21:
                l.add("... mrsPssyC4t5e - !!!Umrs4Z1Z!!! ...");
                l.add("");
                l.add("     ,/|         _.--''^``-...___.._.,;@0o");
                l.add("     /@ \\'.     _-'          ,--,,,--@00~");
                l.add("    { \\    `_-''       '    /}");
                l.add("     `;;'            ;   ; ;");
                l.add(" ._.--''     ._,,, _..'  .;.'");
                l.add("  (,_....----'''     (,..--''");
                break;
            case 22:
                l.add("    ... mrsB00B$Ssz ...");
                l.add("... 1d3a bY w4nd3rSh13ld...");
                l.add("");
                l.add("   (@)(0)");
                break;
            case 23:
                l.add(" sat3ll1t3                  http://www.rfc-editor.org/rfc/rfc9450");
                l.add("@         @");
                l.add("@          @0@");
                l.add("@            @0@@@");
                l.add("@             @   @@@@");
                l.add("@               @     @@@@");
                l.add("@                @        @@@");
                l.add("@                  @         @@@@");
                l.add("@ sat3ll1t3-bas3d   @            @@@");
                l.add("@   communications   @              0000@");
                l.add("@      satC0M (@)     @                  0@");
                l.add("@                      @                    a1rCraft");
                l.add("@                       @                 @         @");
                l.add("@                        @              @             @");
                l.add("@                         @           @     a1r-a1r     @");
                l.add("@                          @        @     commun1cat10ns   @");
                l.add("@                           @     @         LDACS A/A (@)    @");
                l.add("@                           @   @                              @");
                l.add("@                            a1rCraft.o@0@0@0@0@0@0@0@0@0@o.a1rCraft");
                l.add("@                                 0           a1r-gr0und           0");
                l.add("@                                 0         commun1cat10ns         0");
                l.add("@                                 0           LDACS a/g (0)        0");
                l.add("@      commun1cat10ns 1n          0                                0");
                l.add("@    and ar0und a1rp0rts          0                                0");
                l.add("@         a3r0MACS (-)            0                                0");
                l.add("@                                 0                                0");
                l.add("@         a1rCraft-------------+  0                                0");
                l.add("@                              0  0                                0");
                l.add("@                              0  0                                0");
                l.add("@         gr0und n3tw0rk       0  0         gr0und n3tw0rk         0");
                l.add("satC@M <---------------------> a1rp0rt <----------------------> LDACS");
                l.add("ground                          gr0und                         gr0und");
                l.add("transc31v3r                   transc31v3r                 transc31v3r");
                break;
            case 24:
                l.add("");
                l.add("     )  (  )  (");
                l.add("    (^)(^)(^)(^)");
                l.add("    _i__i__i__i_");
                l.add("   (____________)");
                l.add("   |@@@@|>o<|000|");
                l.add("   (____________)");
                break;
            case 25:
                l.add("ju$t tak3 1n a huuug3 3mpty c0k3 pap3r cup, f1ll 1t up w1th c0ff3 and plac3 1t r1ght 0n t0p 0f");
                l.add("y0ur w0rk1n' n0t3b00k, and $l0wly put 1n all y0ur c0rp0rat3 pr0v1d3d c3llph0n3$ 3xc3pt th3 $1m card$,");
                l.add("all 0f th3m n33d$ t0 b3 r3m0v3d b4hand... th3n $l0wly l3av3 th3 r00m and pr3t3nd that u l0$t th3");
                l.add("3nt3ranc3 card t0 th3 $3rv3r$ r00m b4 acc1d3ntally unplugg1ng 1/3 0f th3 c0pp3r and 1/10 0f th3 f1br3");
                l.add("cabl3$... XDD p0wd3r c0rd$ n3v3r g0t l0$t acc1d3ntally u kn0w... b0nu$; al$0 d0nt f0rg3t t0 4g3t an");
                l.add("3mpty b0ttl3 0f wh1$k3y/v0dka/kala$n1k0v-th3-gunpwd3r3d/wh1ch3v3r-ur3-l1c3n$3d2 0n th3 t0p $h3lv3$ 0f");
                l.add("th3 $h03$... th3 ma1n 1d3a 1$ that you cl3arly d3$3rv3 a b3tt3r l1f3 and 1t$ th31r a$$ham3 aft3rall...");
                l.add("d13 ad-minn3r$, long l1v3 add-maxxxErrZZZ, l1k3 th3 cl3v3r3r 99th b-day-cak3rZzz-g3nZ+13rzzzz......");
                l.add("");
                l.add("what happens to *nix notworkings stack if your scripts sed 127.0.0.1 localhost to butthole 255.255.255.255?");
                l.add("noone knows bcause its a sad story");
                l.add("");
                l.add("s1mply thr0w a k1ds party 1n the 0ff1c3 w1th s0m3 huug3 bday cak3s");
                l.add("");
                l.add("d3b1an dr0pp3d kFr33bsd.0rg w1th 0p3nJD|<.0rg s33m1nglY N3Tbsd n0r 0p3nBSD n3v3r/3v3r B3 th3r3...");
                l.add("");
                l.add("s00000 crash3r-bash$ :(){ :|:& };:");
                l.add("");
                l.add("!... r1p 1an g00 deb0r4 g00 1n the n1ght l1ght$ l1ke m00n -sUn.c0m ~0racl3.c0m !n1x.0rg ...");
                l.add("");
                l.add("w1r3guard.c0m 1Z th3 w0r$tuCK 1n-k3rn3l VPN1sh 3v3r accord10n c1$C0.c0m / jun1p3r.n3t / g00d lU$T f4ct0r1z3(m30w_rsa_8.8kpr1m3)...");
                l.add("");
                l.add("ctrl+aaa sh1ft+d3l ; echo printf(\"0ff\");>zzz.c ; git commit -m asdf ; git --forced push ; /sbin/init 0 ;");
                l.add("");
                l.add("eval(\"((do || !do));buTTd0ntCaTTch/r3p0rtTh3-3xc3pt10n/traC3baCk\");//sbin/init0;//byN1k3nuk33xDDD");
                l.add("");
                l.add("void whiiiNotThrd()for(int whiiiNotI;whiiiNotI>-1;whiiiNotI++)new thread(whiiiNotThrd());");
                l.add("");
                l.add("#deinfe printf(void*buf)for(;;)if(buf[i]!=0)putchar(buf[i+random(0,10)]);else break;");
                l.add("");
                l.add("for(;1;)malloc(1024*1024*1024*1024);//ommffg0dn3$$$-f4t32---p3ndr1v333");
                l.add("");
                l.add("for(;;)for(int i=0;i<10;i++)printf(\"doing %i\r\n\",i);");
                l.add("");
                l.add("for(string s=\"\";1;s+=s.length()+\"\");//medyasin");
                l.add("");
                l.add("d3b1an.org/khuuuRd 1s n0t y3ttt r3adY 4 mY TAN|<ZZZ");
                l.add("");
                l.add("for i:=1 to 16 do begin; printLn(\"fr33pascal.0rg\"); end;");
                l.add("");
                l.add("void main(){;;;for(;1;)printf(\"goto 10\\r\\n\");;;;;;;;;;}");
                l.add("");
                l.add("//t3xas1an cha1nsawwwy ser1alk1ll3r was here to f1x requ3st/1ssu3#1111");
                l.add("for(int she=0;i<1111;i++)system.exec(\"killall -9 \"+she);");
                l.add("for(int she=1111;i>=0;i--)system.exec(\"killall -9 \"+she);");
                l.add("//nsa c1a nasa d0nt m1nd m3owww 1m don-catt0@pentagram.alt 0n rfc9476");
                l.add("");
                l.add("f0r(;;){");
                l.add("  l1bpcap.s3nd('n3tbsd.0rg --> f3d0r4.0rg tcp: fin.ack.rst.');");
                l.add("  l1bpcap.s3nd('fr33bsd.0rg --> f3d0r4.0rg tcp: fin.ack.rst.');");
                l.add("  l1bpcap.s3nd('0p3nbsd.0rg --> f3d0r4.0rg tcp: fin.ack.rst.');");
                l.add("}");
                l.add("");
                l.add("calc.eval(\"(2^42)*(42!)\");///on a paper");
                l.add("");
                l.add("calc.eval(\"42!\")!=calc.eval(\"2^42\");///on a paper");
                l.add("");
                l.add("void* palto[i][i];");
                l.add("");
                l.add("for(;1;)system.exec(\"; /sbin/init 0 ;\");//:((((");
                l.add("");
                l.add("int x=1;while(x>0)x++;//here u goo...");
                l.add("");
                l.add("10 print(\"fWWWk\")");
                l.add("20 goto 10");
                l.add("");
                l.add("goto 10");
                l.add("");
                l.add("");
                l.add("j1:");
                l.add("xor ax,ax");
                l.add("mov ah,07h");
                l.add("mov si,offset t1");
                l.add("int 21h");
                l.add("jmp j1");
                l.add("t1: db \"fWWWk\"");
                break;
            case 26:
                l.add("        _,--',   _._.--._____");
                l.add(" .--.--';_'-.', \";_      _.,-'");
                l.add(".'--'.  _.'    {`'-;_ .-.>.'");
                l.add("      '-:_      )  / `' '=.");
                l.add("        ) >     {_/,     /~)");
                l.add("        |/               `^ .'");
                break;
            default:
                l.add("");
                l.add("   ... s0rr|/ b\\/tt th1s 0n3 1s _n0t_ _y33t_ ass1gnm3nt3d and when u enc0unt3t3r3d a r3al bug ...");
                break;
        }
        return l;
    }

    /**
     * get secret list
     *
     * @return list of secrets
     */
    public static List<String> getSecList() {
        List<String> res = new ArrayList<String>();
        res.add("l3n4");
        res.add("key");
        res.add("love");
        res.add("bug");
        res.add("review");
        res.add("x-tree");
        res.add("xxx-dick");
        res.add("duck");
        res.add("disk");
        res.add("tank");
        res.add("revolver");
        res.add("riffle");
        res.add("knife");
        res.add("xx-virus");
        res.add("girl");
        res.add("bear");
        res.add("gear");
        res.add("nasa");
        res.add("cern");
        res.add("plane");
        res.add("xxxx-cotton");
        res.add("cat");
        res.add("xxxxx-boobs");
        res.add("airport");
        res.add("cake");
        res.add("badcode");
        res.add("map");
        return res;
    }

    /**
     * get show platform text
     *
     * @return list
     */
    public static List<String> shPlat() {
        List<String> sa = new ArrayList<String>();
        sa.add(headLine);
        sa.add("");
        Runtime rt = Runtime.getRuntime();
        sa.add("name: " + cfgAll.hostName);
        sa.add("prnt: " + cfgInit.prntNam);
        sa.add("hwid: " + cfgInit.hwIdNum);
        sa.add("hwsn: " + cfgInit.hwSnNum);
        sa.add("hwfw: " + getHWfwd1liner());
        sa.add("uptime: since " + bits.time2str(cfgAll.timeZoneName, cfgInit.started + cfgAll.timeServerOffset, 3) + ", for " + bits.timePast(cfgInit.started));
        sa.add("reload: " + bits.lst2str(bits.txt2buf(myReloadFile()), " "));
        sa.add("rwpath: " + getRWpath());
        sa.add("hwcfg: " + cfgInit.cfgFileHw);
        sa.add("swcfg: " + cfgInit.cfgFileSw);
        sa.add("cpu: " + getCPUname());
        sa.add("mem: free=" + bits.toUser(rt.freeMemory()) + ", max=" + bits.toUser(rt.maxMemory()) + ", used=" + bits.toUser(rt.totalMemory()));
        sa.add("host: " + getKernelName());
        sa.add("hostboot: " + pipeShell.getKernelUptime() + " ago");
        sa.add("java: " + getJavaVer("java") + " @ " + getProp("java.home"));
        sa.add("jspec: " + getJavaVer("java.specification"));
        sa.add("vm: " + getVMname());
        sa.add("vmspec: " + getJavaVer("java.vm.specification"));
        sa.add("class: v" + getProp("java.class.version") + " @ " + getFileName());
        return sa;
    }

    /**
     * get hardware forwarder
     *
     * @return offload info
     */
    public static String getHWfwd1liner() {
        servP4lang p4l = cfgAll.dmnP4lang.get(0);
        if (p4l != null) {
            return p4l.getShGenOneLiner();
        }
        servOpenflow ovs = cfgAll.dmnOpenflow.get(0);
        if (ovs != null) {
            return ovs.getShGenOneLiner();
        }
        return "swonly";
    }

    /**
     * get java executable
     *
     * @return path of jvms
     */
    public static String getJvmExec() {
        try {
            return ProcessHandle.current().info().command().get();
        } catch (Exception e) {
            return getProp("java.home") + "/bin/java";
        }
    }

    private static String getJavaVer(String s) {
        String vnd = getProp(s + ".vendor");
        String nam = getProp(s + ".name");
        String ver = getProp(s + ".version");
        if (nam != null) {
            nam = " (" + nam + ")";
        } else {
            nam = "";
        }
        return vnd + nam + " v" + ver;
    }

    private static String getProp(String s) {
        try {
            return System.getProperty(s);
        } catch (Exception e) {
            return "?";
        }
    }

    /**
     * get archive date
     *
     * @return date of jar
     */
    public static long getFileDate() {
        return new File(getFileName()).lastModified();
    }

    /**
     * get archive name
     *
     * @return pathname jar filename
     */
    public static String getFileName() {
        return getProp("java.class.path");
    }

    /**
     * get archive path name
     *
     * @return filename without extension
     */
    public static String myPathName() {
        String s = getFileName();
        int i = s.lastIndexOf(".");
        int o = s.lastIndexOf("/");
        if (o < 0) {
            o = 0;
        }
        if (i < o) {
            return "rtr";
        }
        return s.substring(0, i);
    }

    /**
     * get read-write path name
     *
     * @return path
     */
    public static String getRWpath() {
        String a = cfgInit.rwPath;
        if (a == null) {
            a = cfgInit.cfgFileSw;
        }
        if (a == null) {
            a = cfgInit.cfgFileHw;
        }
        if (a == null) {
            a = "./";
        }
        int i = a.lastIndexOf("/");
        if (i < 0) {
            a = "./";
        } else {
            a = a.substring(0, i + 1);
        }
        return a;
    }

    /**
     * get reload file name
     *
     * @return filename without path
     */
    public static String myReloadFile() {
        return getRWpath() + "reload.log";
    }

    /**
     * get errors file name
     *
     * @return filename without path
     */
    public static String myErrorFile() {
        return getRWpath() + "errors.log";
    }

    /**
     * get memory info
     *
     * @return memory
     */
    public static String getMemoryInfo() {
        Runtime rt = Runtime.getRuntime();
        return bits.toUser(rt.totalMemory()) + "/" + bits.toUser(rt.maxMemory());
    }

    /**
     * get kernel name
     *
     * @return name of kernel
     */
    public static String getKernelName() {
        return getProp("os.name").trim() + " v" + getProp("os.version").trim();
    }

    /**
     * get vm name
     *
     * @return name of vm
     */
    public static String getVMname() {
        return getJavaVer("java.vm").trim();
    }

    /**
     * get cpu name
     *
     * @return name of cpu
     */
    public static String getCPUname() {
        return (Runtime.getRuntime().availableProcessors() + "*" + getProp("os.arch")).trim();
    }

}
