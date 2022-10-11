package me.dio.sacola.service;

import lombok.RequiredArgsConstructor;
import me.dio.sacola.enumeration.FormaPagamento;
import me.dio.sacola.model.Item;
import me.dio.sacola.model.Restaurante;
import me.dio.sacola.model.Sacola;
import me.dio.sacola.repository.ItemRepository;
import me.dio.sacola.repository.ProdutoRepository;
import me.dio.sacola.repository.SacolaRepository;
import me.dio.sacola.resource.dto.ItemDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class sacolaServiceImpl implements SacolaService{
    private final SacolaRepository sacolaRepository;
    private final ProdutoRepository produtoRepository;


    @Override
    public Item incluirItemNaSacola(ItemDto itemDto) {
        Sacola sacola = verSacola(itemDto.getIdSacola());

        if(sacola.isFechada()){
            throw new RuntimeException("Esta sacola está fechada.");
        }

        Item novoItem = Item.builder()
                .qtd(itemDto.getQtd())
                .sacola(sacola)
                .produto(produtoRepository.findById(itemDto.getIdProduto()).orElseThrow(
                        () -> {
                            throw new RuntimeException("Esse produto não existe!");
                        }
                ))
                .build();

        List<Item> itensDaSacola = sacola.getItens();


        if(itensDaSacola.isEmpty()){
            itensDaSacola.add(novoItem);
        }else {
            Restaurante restauranteAtual = itensDaSacola.get(0).getProduto().getRestaurante();
            Restaurante restauranteNovoItem = novoItem.getProduto().getRestaurante();

            if(restauranteAtual.equals(restauranteNovoItem))
                itensDaSacola.add(novoItem);
            else
                throw new RuntimeException("Não é possível adicionar produtos de restaurantes diferentes. Feche ou limpe a sacola.");

        }

        List<Double> valorItens = new ArrayList<>();

        for(Item itemDaSacola: itensDaSacola){
            double valorTotalItem = itemDaSacola.getProduto().getValorUnitario() * itemDaSacola.getQtd();
            valorItens.add(valorTotalItem);
        }

        double valorTotalSacola = valorItens.stream()
                .mapToDouble(valorTotalCadaItem -> valorTotalCadaItem)
                .sum();

        sacola.setValorTotal(valorTotalSacola);
        sacolaRepository.save(sacola);
        return novoItem;

    }

    @Override
    public Sacola verSacola(Long id) {
        return sacolaRepository.findById(id).orElseThrow(
                () -> {
                    throw new RuntimeException("Essa sacola não existe!");
            }
        );
    }

    @Override
    public Sacola fechaSacola(Long id, int numeroformaPagamento) {
        Sacola sacola = verSacola(id);

        if(sacola.getItens().isEmpty()){
            throw new RuntimeException("Inclua itens na sacola!");
        }

        FormaPagamento formaPagamento = numeroformaPagamento == 0 ? FormaPagamento.DINHEIRO : FormaPagamento.MAQUINETA;

        sacola.setFormaPagamento(formaPagamento);
        sacola.setFechada(true);

        return sacolaRepository.save(sacola);
    }
}
