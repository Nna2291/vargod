// View layer — журнал аудита (admin) с фильтрами и пагинацией.

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, RefreshCw } from 'lucide-react';
import * as adminApi from '@/model/api/admin';
import { Button } from '@/view/components/ui/button';
import { Card, CardContent } from '@/view/components/ui/card';
import { Input } from '@/view/components/ui/input';
import { Label } from '@/view/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/view/components/ui/table';

const PAGE = 25;

export function AdminAuditPage() {
  const [filters, setFilters] = useState({
    action: '',
    userId: '',
    from: '',
    to: '',
  });
  const [offset, setOffset] = useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);

  // datetime-local emits "YYYY-MM-DDTHH:mm" без секунд и часового пояса —
  // конвертируем в ISO-8601 (UTC) для бэкенда, который парсит Instant.
  const toIso = (local: string): string | undefined => {
    if (!local) return undefined;
    const d = new Date(local);
    return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
  };

  const query = useQuery({
    queryKey: ['admin', 'audit', filters, offset],
    queryFn: () =>
      adminApi.listAudit({
        action: filters.action || undefined,
        userId: filters.userId || undefined,
        from: toIso(filters.from),
        to: toIso(filters.to),
        limit: PAGE,
        offset,
      }),
  });

  const applyFilter = (k: keyof typeof filters, v: string) => {
    setOffset(0);
    setFilters((prev) => ({ ...prev, [k]: v }));
  };

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">Аудит</h1>
        <p className="text-muted-foreground">Журнал действий пользователей.</p>
      </header>

      <Card>
        <CardContent className="grid gap-4 p-6 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-2">
            <Label htmlFor="filter-action">Действие</Label>
            <Input
              id="filter-action"
              value={filters.action}
              onChange={(e) => applyFilter('action', e.target.value)}
              placeholder="USER_CREATED"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="filter-userId">ID пользователя</Label>
            <Input
              id="filter-userId"
              value={filters.userId}
              onChange={(e) => applyFilter('userId', e.target.value)}
              placeholder="UUID"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="filter-from">С (ISO-8601)</Label>
            <Input
              id="filter-from"
              type="datetime-local"
              value={filters.from}
              onChange={(e) => applyFilter('from', e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="filter-to">По (ISO-8601)</Label>
            <Input
              id="filter-to"
              type="datetime-local"
              value={filters.to}
              onChange={(e) => applyFilter('to', e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => query.refetch()}>
          <RefreshCw className="mr-2 h-4 w-4" />
          Обновить
        </Button>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">
            Страница {Math.floor(offset / PAGE) + 1}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={offset === 0}
            onClick={() => setOffset((o) => Math.max(0, o - PAGE))}
          >
            <ChevronLeft className="mr-1 h-4 w-4" /> Назад
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={(query.data?.length ?? 0) < PAGE}
            onClick={() => setOffset((o) => o + PAGE)}
          >
            Вперёд <ChevronRight className="ml-1 h-4 w-4" />
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          {query.isLoading ? (
            <p className="p-6 text-sm text-muted-foreground">Загрузка…</p>
          ) : !query.data || query.data.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">Записей нет.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[180px]">Время</TableHead>
                  <TableHead>Действие</TableHead>
                  <TableHead>Цель</TableHead>
                  <TableHead>Пользователь</TableHead>
                  <TableHead className="w-[60px]" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {query.data.map((e) => {
                  const isOpen = expanded === e.id;
                  return (
                    <React.Fragment key={e.id}>
                      <TableRow>
                        <TableCell className="font-mono text-xs">
                          {new Date(e.createdAt).toLocaleString('ru-RU')}
                        </TableCell>
                        <TableCell className="font-medium">{e.action}</TableCell>
                        <TableCell className="text-muted-foreground">
                          {e.targetType ?? '—'}
                          {e.targetId && (
                            <code className="ml-1 font-mono text-xs">
                              {e.targetId.slice(0, 8)}
                            </code>
                          )}
                        </TableCell>
                        <TableCell className="font-mono text-xs text-muted-foreground">
                          {e.userId?.slice(0, 8) ?? '—'}
                        </TableCell>
                        <TableCell>
                          {e.payload && (
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => setExpanded(isOpen ? null : e.id)}
                              aria-label="Показать payload"
                            >
                              {isOpen ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              )}
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                      {isOpen && e.payload && (
                        <TableRow>
                          <TableCell colSpan={5} className="bg-muted/30">
                            <pre className="overflow-auto rounded bg-muted p-3 text-xs">
                              {JSON.stringify(e.payload, null, 2)}
                            </pre>
                          </TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
