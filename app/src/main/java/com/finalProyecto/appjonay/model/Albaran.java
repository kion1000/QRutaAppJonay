package com.finalProyecto.appjonay.models;

public class Albaran {
    private String numeroAlbaran, fecha,
            nombreCliente, apellidosCliente,
            direccion, telefono, email,
            dni, cif, producto, observaciones, estado;
    private int cantidad;

    // Constructor vacío (Firestore / Gson lo necesitan)
    public Albaran() {}

    // Constructor completo
    public Albaran(String numeroAlbaran, String fecha,
                   String nombreCliente, String apellidosCliente,
                   String direccion, String telefono, String email,
                   String dni, String cif, String producto,
                   int cantidad, String observaciones, String estado) {
        this.numeroAlbaran = numeroAlbaran;
        this.fecha = fecha;
        this.nombreCliente = nombreCliente;
        this.apellidosCliente = apellidosCliente;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.dni = dni;
        this.cif = cif;
        this.producto = producto;
        this.cantidad = cantidad;
        this.observaciones = observaciones;
        this.estado = estado;
    }

    // Getters & setters...
    public String getNumeroAlbaran() { return numeroAlbaran; }
    public void setNumeroAlbaran(String n) { this.numeroAlbaran = n; }
    // … repite para cada campo …
    public int getCantidad() { return cantidad; }
    public void setCantidad(int c) { this.cantidad = c; }
}
