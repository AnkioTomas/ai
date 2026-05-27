package net.ankio.ai.lib.test

/** 连接测试用 demo 图（JPEG），图中文字为 AUTO TEST */
internal object AiTestDemo {
    val IMAGE_BASE64: String =
        "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAA" +
                "A6ABAAMAAAABAAEAAKACAAQAAAABAAAA/KADAAQAAAABAAAARAAAAAD/7QA4UGhvdG9zaG9wIDMu" +
                "MAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgARAD8AwEiAAIR" +
                "AQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAAB" +
                "fQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5" +
                "OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeo" +
                "qaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMB" +
                "AQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYS" +
                "QVEHYXETIjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNU" +
                "VVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5" +
                "usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwICAwUDAwMF" +
                "BgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwMDA4ODg4ODw8PDw8PDw8PD//bAEMBAgIC" +
                "BAQEBwQEBxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ" +
                "EBAQEP/dAAQAEP/aAAwDAQACEQMRAD8A/fyiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo" +
                "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigD/9D9/KKKKACvwB/4fnf9UT/8uT/720f8" +
                "Fzv+aJ/9zJ/7ja/f6gD4A/YY/bn/AOG0f+E2/wCKJ/4Q7/hDv7N/5iX9ofaf7Q+0/wDTtb7Nn2f/" +
                "AGt27tjk/bn/AG5/+GLv+EJ/4on/AITH/hMf7S/5iX9n/Zv7P+zf9O1xv3/aP9nbt754+AP+C53/" +
                "ADRP/uZP/cbR/wAFzv8Amif/AHMn/uNoA/f6vgD9uf8Abn/4Yu/4Qn/iif8AhMf+Ex/tL/mJf2f9" +
                "m/s/7N/07XG/f9o/2du3vnj7/r8Af+C53/NE/wDuZP8A3G0Aff8A+3P+3P8A8MXf8IT/AMUT/wAJ" +
                "j/wmP9pf8xL+z/s39n/Zv+na437/ALR/s7dvfPHwB/w/O/6on/5cn/3tr9/q/AH/AILnf80T/wC5" +
                "k/8AcbQAf8Pzv+qJ/wDlyf8A3tr9/qK/AH/gud/zRP8A7mT/ANxtAB/w/O/6on/5cn/3tr7/AP2G" +
                "P25/+G0f+E2/4on/AIQ7/hDv7N/5iX9ofaf7Q+0/9O1vs2fZ/wDa3bu2Ofv+vwB/4Lnf80T/AO5k" +
                "/wDcbQB+/wBRRRQAV8Aftz/tz/8ADF3/AAhP/FE/8Jj/AMJj/aX/ADEv7P8As39n/Zv+na437/tH" +
                "+zt2988ff9FAH4A/8Pzv+qJ/+XJ/97aP+H53/VE//Lk/+9tH/Bc7/mif/cyf+42v3+oAKKKKACii" +
                "igAooooA+AP25/25/wDhi7/hCf8Aiif+Ex/4TH+0v+Yl/Z/2b+z/ALN/07XG/f8AaP8AZ27e+ePg" +
                "D/h+d/1RP/y5P/vbX7/V+AP/AAXO/wCaJ/8Acyf+42gA/wCH53/VE/8Ay5P/AL21+/1FFABRRRQB" +
                "/9H9/KKKKAPwB/4Lnf8ANE/+5k/9xtH/AA4x/wCq2f8Alt//AHyo/wCC53/NE/8AuZP/AHG1+/1A" +
                "H8gX7c/7DH/DF3/CE/8AFbf8Jj/wmP8AaX/MN/s/7N/Z/wBm/wCnm437/tH+zt2988ff/wDwXO/5" +
                "on/3Mn/uNo/4Lnf80T/7mT/3G19//tz/ALDH/DaP/CE/8Vt/wh3/AAh39pf8w3+0PtP9ofZv+nm3" +
                "2bPs/wDtbt3bHIB9/wBfgD/wXO/5on/3Mn/uNo/4fnf9UT/8uT/720f8po/+qO/8Kd/7mH+0f+Eh" +
                "/wDBf5Hkf2d/003+Z/Bs+YA/f6vwB/4Lnf8ANE/+5k/9xtfv9X4A/wDBc7/mif8A3Mn/ALjaAP3+" +
                "r8Af+C53/NE/+5k/9xtfv9XwB+3P+wx/w2j/AMIT/wAVt/wh3/CHf2l/zDf7Q+0/2h9m/wCnm32b" +
                "Ps/+1u3dscgH3/X4A/8ABc7/AJon/wBzJ/7jaP8Ah+d/1RP/AMuT/wC9tH/KaP8A6o7/AMKd/wC5" +
                "h/tH/hIf/Bf5Hkf2d/003+Z/Bs+YA/f6iiigAooooA/AH/gud/zRP/uZP/cbX7/V+AP/AAXO/wCa" +
                "J/8Acyf+42v3+oAKKKKACiiigAooooAK/AH/AILnf80T/wC5k/8AcbX7/V+AP/Bc7/mif/cyf+42" +
                "gD9/qKKKACiiigD/0v38ooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKK" +
                "KACiiigAooooAKKKKACiiigAooooA//T/fyiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo" +
                "AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigD/9k="
}
