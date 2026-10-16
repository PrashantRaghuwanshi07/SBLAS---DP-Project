package project;
class EmailChannel implements NotificationChannel { public void send(String m){System.out.println("[EMAIL DEMO] "+m);} }
class SMSChannel implements NotificationChannel { public void send(String m){System.out.println("[SMS DEMO] "+m);} }
class AppChannel implements NotificationChannel { public void send(String m){System.out.println("[APP DEMO] "+m);} }
