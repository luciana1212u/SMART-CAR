package com.smartcar.backend.models;

public class microcontrolador {

	private String estado;
	private float velocidadActual;
	private String modoOperacion;

	public void procesarDatos() {
	}

	public void ejecutarComando() {
	}

	public void detenerCarro() {
	}

	public void recibirComando(String comando) {
	}

	public void controlarIluminacion() {
	}

	public void monitorearSeguridad() {
	}

	public void controlarMotor() {
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public float getVelocidadActual() {
		return velocidadActual;
	}

	public void setVelocidadActual(float velocidadActual) {
		this.velocidadActual = velocidadActual;
	}

	public String getModoOperacion() {
		return modoOperacion;
	}

	public void setModoOperacion(String modoOperacion) {
		this.modoOperacion = modoOperacion;
	}
}
