{$stack 512}
{$heap 65500}
{$sysinc system.inc}
Type
  OneDrawCharRec=record
    c:char;
    u:byte;
    d:byte;
    l:byte;
    r:byte;
    end;
Const
  DrawChrs:array[0..40] of OneDrawCharRec=(   (c:' ';u:0;d:0;l:0;r:0),
  (c:'�';u:0;d:0;l:1;r:1),(c:'�';u:0;d:0;l:2;r:2),
  (c:'�';u:1;d:1;l:0;r:0),(c:'�';u:2;d:2;l:0;r:0),
  (c:'�';u:0;d:1;l:2;r:2),(c:'�';u:0;d:2;l:1;r:1),
  (c:'�';u:1;d:1;l:2;r:2),(c:'�';u:2;d:2;l:1;r:1),
  (c:'�';u:1;d:0;l:2;r:2),(c:'�';u:2;d:0;l:1;r:1),
  (c:'�';u:0;d:1;l:1;r:1),(c:'�';u:0;d:2;l:2;r:2),
  (c:'�';u:1;d:1;l:1;r:1),(c:'�';u:2;d:2;l:2;r:2),
  (c:'�';u:1;d:0;l:1;r:1),(c:'�';u:2;d:0;l:2;r:2),
  (c:'�';u:0;d:1;l:1;r:0),(c:'�';u:0;d:2;l:2;r:0),
  (c:'�';u:1;d:1;l:1;r:0),(c:'�';u:2;d:2;l:2;r:0),
  (c:'�';u:1;d:0;l:1;r:0),(c:'�';u:2;d:0;l:2;r:0),
  (c:'�';u:0;d:1;l:0;r:1),(c:'�';u:0;d:2;l:0;r:2),
  (c:'�';u:1;d:1;l:0;r:1),(c:'�';u:2;d:2;l:0;r:2),
  (c:'�';u:1;d:0;l:0;r:1),(c:'�';u:2;d:0;l:0;r:2),
  (c:'�';u:0;d:1;l:2;r:0),(c:'�';u:0;d:2;l:1;r:0),
  (c:'�';u:1;d:1;l:2;r:0),(c:'�';u:2;d:2;l:1;r:0),
  (c:'�';u:1;d:0;l:2;r:0),(c:'�';u:2;d:0;l:1;r:0),
  (c:'�';u:0;d:1;l:0;r:2),(c:'�';u:0;d:2;l:0;r:1),
  (c:'�';u:1;d:1;l:0;r:2),(c:'�';u:2;d:2;l:0;r:1),
  (c:'�';u:1;d:0;l:0;r:2),(c:'�';u:2;d:0;l:0;r:1)
  );

Function GetOneChar(x,y:LongInt):Byte;
Begin;
GetOneChar:=x+y;
End;

Var
  ox,oy:LongInt;
  dat:OneDrawCharRec;
BEGIN;
ox:=4;
oy:=3;
dat.l:=DrawChrs[GetOneChar(ox-1,oy)].r;
dat.r:=DrawChrs[GetOneChar(ox+1,oy)].l;
dat.u:=DrawChrs[GetOneChar(ox,oy-1)].d;
dat.d:=DrawChrs[GetOneChar(ox,oy+1)].u;
writeln(BStr(dat.l));
writeln(BStr(dat.r));
writeln(BStr(dat.u));
writeln(BStr(dat.d));
END.