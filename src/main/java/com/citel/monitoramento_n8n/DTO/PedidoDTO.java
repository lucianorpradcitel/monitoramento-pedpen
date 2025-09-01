package com.citel.monitoramento_n8n.DTO;


public class PedidoDTO {
    private String codigoPedido;
    private String cliente;
    private String erro;
    private String plataforma;


    public String getCodigoPedido() {
        return codigoPedido;
    }
    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public String getCliente() {
        return cliente;
    }
    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getErro()
    {
        return erro;
    }

    public void setErro(String erro)
    {
        this.erro = erro;
    }

    public String getPlataforma()
    {
        return plataforma;
    }

    public void setPlataforma(String plataforma)
    {
        this.plataforma = plataforma;
    }


}
