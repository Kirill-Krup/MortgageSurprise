import React, { useEffect, useState } from 'react';
import { scoringRulesApi } from '../api/client';
import { useLanguage } from '../context/LanguageContext';
import type {
  AgeRule,
  CityRule,
  CountryRule,
  GenderRule,
  ScoringRuleType,
} from '../types';
import { getApiErrorMessage } from '../utils/errors';
import {
  AlertCircle,
  Building2,
  Calendar,
  Globe,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  UserCheck,
  X,
} from 'lucide-react';

type RuleCategory = 'age' | 'gender' | 'country' | 'city';

export const AdminScoringRulesSection: React.FC = () => {
  const { language } = useLanguage();
  const isRu = language === 'ru';

  const [category, setCategory] = useState<RuleCategory>('age');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [ageRules, setAgeRules] = useState<AgeRule[]>([]);
  const [genderRules, setGenderRules] = useState<GenderRule[]>([]);
  const [countryRules, setCountryRules] = useState<CountryRule[]>([]);
  const [cityRules, setCityRules] = useState<CityRule[]>([]);

  // Form state
  const [editingId, setEditingId] = useState<number | null>(null);
  const [minAge, setMinAge] = useState('18');
  const [maxAge, setMaxAge] = useState('65');
  const [valueInput, setValueInput] = useState('');
  const [ruleType, setRuleType] = useState<ScoringRuleType>('INCLUSIVE');
  const [creditScore, setCreditScore] = useState('0.2');

  const loadRules = async (cat: RuleCategory = category) => {
    setLoading(true);
    setError(null);
    try {
      if (cat === 'age') {
        const data = await scoringRulesApi.getAgeRules();
        setAgeRules(data.sort((a, b) => a.id - b.id));
      } else if (cat === 'gender') {
        const data = await scoringRulesApi.getGenderRules();
        setGenderRules(data.sort((a, b) => a.id - b.id));
      } else if (cat === 'country') {
        const data = await scoringRulesApi.getCountryRules();
        setCountryRules(data.sort((a, b) => a.id - b.id));
      } else {
        const data = await scoringRulesApi.getCityRules();
        setCityRules(data.sort((a, b) => a.id - b.id));
      }
    } catch (err) {
      setError(getApiErrorMessage(err, isRu ? 'Не удалось загрузить правила скоринга' : 'Failed to load scoring rules'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    resetForm();
    loadRules(category);
  }, [category]);

  const resetForm = () => {
    setEditingId(null);
    setMinAge('18');
    setMaxAge('65');
    setValueInput('');
    setRuleType('INCLUSIVE');
    setCreditScore('0.2');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const parsedScore = parseFloat(creditScore);
    if (Number.isNaN(parsedScore)) {
      setError(isRu ? 'Введите корректный балл (creditScore)' : 'Enter a valid credit score');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      if (category === 'age') {
        const payload = {
          minAge: parseInt(minAge, 10),
          maxAge: parseInt(maxAge, 10),
          ruleType,
          creditScore: parsedScore,
        };
        if (editingId !== null) {
          await scoringRulesApi.updateAgeRule(editingId, payload);
        } else {
          await scoringRulesApi.createAgeRule(payload);
        }
      } else if (category === 'gender') {
        const payload = { name: valueInput.trim(), ruleType, creditScore: parsedScore };
        if (editingId !== null) {
          await scoringRulesApi.updateGenderRule(editingId, payload);
        } else {
          await scoringRulesApi.createGenderRule(payload);
        }
      } else if (category === 'country') {
        const payload = { countryCode: valueInput.trim().toUpperCase(), ruleType, creditScore: parsedScore };
        if (editingId !== null) {
          await scoringRulesApi.updateCountryRule(editingId, payload);
        } else {
          await scoringRulesApi.createCountryRule(payload);
        }
      } else {
        const payload = { cityName: valueInput.trim(), ruleType, creditScore: parsedScore };
        if (editingId !== null) {
          await scoringRulesApi.updateCityRule(editingId, payload);
        } else {
          await scoringRulesApi.createCityRule(payload);
        }
      }
      resetForm();
      await loadRules(category);
    } catch (err) {
      setError(getApiErrorMessage(err, isRu ? 'Ошибка при сохранении правила' : 'Failed to save rule'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    setError(null);
    try {
      if (category === 'age') await scoringRulesApi.deleteAgeRule(id);
      else if (category === 'gender') await scoringRulesApi.deleteGenderRule(id);
      else if (category === 'country') await scoringRulesApi.deleteCountryRule(id);
      else await scoringRulesApi.deleteCityRule(id);
      if (editingId === id) resetForm();
      await loadRules(category);
    } catch (err) {
      setError(getApiErrorMessage(err, isRu ? 'Ошибка при удалении правила' : 'Failed to delete rule'));
    }
  };

  const startEditAge = (r: AgeRule) => {
    setEditingId(r.id);
    setMinAge(String(r.minAge));
    setMaxAge(String(r.maxAge));
    setRuleType(r.ruleType);
    setCreditScore(String(r.creditScore));
  };

  const startEditStringRule = (id: number, val: string, rt: ScoringRuleType, score: number) => {
    setEditingId(id);
    setValueInput(val);
    setRuleType(rt);
    setCreditScore(String(score));
  };

  const tabs: Array<{ id: RuleCategory; label: string; icon: React.ReactNode }> = [
    { id: 'age', label: isRu ? 'Возраст' : 'Age', icon: <Calendar size={15} /> },
    { id: 'gender', label: isRu ? 'Гендер' : 'Gender', icon: <UserCheck size={15} /> },
    { id: 'country', label: isRu ? 'Страна' : 'Country', icon: <Globe size={15} /> },
    { id: 'city', label: isRu ? 'Город' : 'City', icon: <Building2 size={15} /> },
  ];

  return (
    <div className="space-y-6">
      {/* Category Switcher */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl p-4 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {tabs.map((tab) => {
            const active = category === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setCategory(tab.id)}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-2 ${
                  active
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {tab.icon}
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        <button
          type="button"
          onClick={() => loadRules(category)}
          disabled={loading}
          className="px-3.5 py-2 rounded-xl text-xs font-bold bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span>{isRu ? 'Обновить' : 'Refresh'}</span>
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800/60 text-rose-700 dark:text-rose-300 px-4 py-3 rounded-2xl text-xs font-semibold flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
          <button type="button" onClick={() => setError(null)} className="cursor-pointer">
            <X size={14} />
          </button>
        </div>
      )}

      {/* Create / Edit Form */}
      <form
        onSubmit={handleSubmit}
        className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4"
      >
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">
            {editingId !== null
              ? isRu
                ? `Редактирование правила #${editingId}`
                : `Edit Rule #${editingId}`
              : isRu
                ? 'Добавить правило скоринга'
                : 'Add Scoring Rule'}
          </h3>
          {editingId !== null && (
            <button
              type="button"
              onClick={resetForm}
              className="text-xs font-semibold text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 flex items-center gap-1 cursor-pointer"
            >
              <X size={14} />
              <span>{isRu ? 'Отмена' : 'Cancel'}</span>
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {category === 'age' ? (
            <>
              <div>
                <label className="block text-[11px] font-bold uppercase text-slate-400 mb-1">
                  {isRu ? 'Мин. возраст' : 'Min Age'}
                </label>
                <input
                  type="number"
                  required
                  value={minAge}
                  onChange={(e) => setMinAge(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm"
                />
              </div>
              <div>
                <label className="block text-[11px] font-bold uppercase text-slate-400 mb-1">
                  {isRu ? 'Макс. возраст' : 'Max Age'}
                </label>
                <input
                  type="number"
                  required
                  value={maxAge}
                  onChange={(e) => setMaxAge(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm"
                />
              </div>
            </>
          ) : (
            <div className="sm:col-span-2">
              <label className="block text-[11px] font-bold uppercase text-slate-400 mb-1">
                {category === 'gender'
                  ? isRu ? 'Гендер (например, MALE / FEMALE)' : 'Gender (e.g. MALE / FEMALE)'
                  : category === 'country'
                    ? isRu ? 'Код страны (ISO-2/3, например RU)' : 'Country Code (e.g. RU)'
                    : isRu ? 'Город (например, Moscow)' : 'City Name (e.g. Moscow)'}
              </label>
              <input
                type="text"
                required
                maxLength={category === 'country' ? 3 : 100}
                value={valueInput}
                onChange={(e) => setValueInput(e.target.value)}
                placeholder={category === 'gender' ? 'FEMALE' : category === 'country' ? 'RU' : 'Moscow'}
                className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm"
              />
            </div>
          )}

          <div>
            <label className="block text-[11px] font-bold uppercase text-slate-400 mb-1">
              {isRu ? 'Тип правила' : 'Rule Type'}
            </label>
            <select
              value={ruleType}
              onChange={(e) => setRuleType(e.target.value as ScoringRuleType)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm"
            >
              <option value="INCLUSIVE">INCLUSIVE</option>
              <option value="EXCLUSIVE">EXCLUSIVE</option>
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase text-slate-400 mb-1">
              {isRu ? 'Балл (creditScore)' : 'Credit Score'}
            </label>
            <input
              type="number"
              step="0.01"
              required
              value={creditScore}
              onChange={(e) => setCreditScore(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm"
            />
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-5 py-2.5 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <Plus size={15} />
            <span>
              {saving
                ? isRu ? 'Сохранение...' : 'Saving...'
                : editingId !== null
                  ? isRu ? 'Сохранить изменения' : 'Update Rule'
                  : isRu ? 'Добавить правило' : 'Create Rule'}
            </span>
          </button>
        </div>
      </form>

      {/* Rules Table */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 dark:bg-slate-800/60 text-[11px] uppercase text-slate-400 font-bold">
              <tr>
                <th className="px-5 py-3.5">ID</th>
                <th className="px-5 py-3.5">
                  {category === 'age'
                    ? isRu ? 'Возрастной диапазон' : 'Age Range'
                    : isRu ? 'Значение' : 'Value'}
                </th>
                <th className="px-5 py-3.5">{isRu ? 'Тип' : 'Type'}</th>
                <th className="px-5 py-3.5">{isRu ? 'Балл (Score)' : 'Score'}</th>
                <th className="px-5 py-3.5 text-right">{isRu ? 'Действия' : 'Actions'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-xs">
              {category === 'age' &&
                ageRules.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40">
                    <td className="px-5 py-3 font-mono text-slate-400">#{r.id}</td>
                    <td className="px-5 py-3 font-semibold">{r.minAge} – {r.maxAge}</td>
                    <td className="px-5 py-3">
                      <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 font-mono text-[11px]">
                        {r.ruleType}
                      </span>
                    </td>
                    <td className={`px-5 py-3 font-bold ${r.creditScore >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                      {r.creditScore}
                    </td>
                    <td className="px-5 py-3 text-right space-x-2">
                      <button type="button" onClick={() => startEditAge(r)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-blue-600 cursor-pointer">
                        <Pencil size={14} />
                      </button>
                      <button type="button" onClick={() => handleDelete(r.id)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-rose-600 cursor-pointer">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}

              {category === 'gender' &&
                genderRules.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40">
                    <td className="px-5 py-3 font-mono text-slate-400">#{r.id}</td>
                    <td className="px-5 py-3 font-semibold">{r.gender}</td>
                    <td className="px-5 py-3">
                      <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 font-mono text-[11px]">
                        {r.ruleType}
                      </span>
                    </td>
                    <td className={`px-5 py-3 font-bold ${r.creditScore >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                      {r.creditScore}
                    </td>
                    <td className="px-5 py-3 text-right space-x-2">
                      <button type="button" onClick={() => startEditStringRule(r.id, r.gender, r.ruleType, r.creditScore)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-blue-600 cursor-pointer">
                        <Pencil size={14} />
                      </button>
                      <button type="button" onClick={() => handleDelete(r.id)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-rose-600 cursor-pointer">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}

              {category === 'country' &&
                countryRules.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40">
                    <td className="px-5 py-3 font-mono text-slate-400">#{r.id}</td>
                    <td className="px-5 py-3 font-semibold">{r.countryName}</td>
                    <td className="px-5 py-3">
                      <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 font-mono text-[11px]">
                        {r.ruleType}
                      </span>
                    </td>
                    <td className={`px-5 py-3 font-bold ${r.creditScore >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                      {r.creditScore}
                    </td>
                    <td className="px-5 py-3 text-right space-x-2">
                      <button type="button" onClick={() => startEditStringRule(r.id, r.countryName, r.ruleType, r.creditScore)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-blue-600 cursor-pointer">
                        <Pencil size={14} />
                      </button>
                      <button type="button" onClick={() => handleDelete(r.id)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-rose-600 cursor-pointer">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}

              {category === 'city' &&
                cityRules.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40">
                    <td className="px-5 py-3 font-mono text-slate-400">#{r.id}</td>
                    <td className="px-5 py-3 font-semibold">{r.cityName}</td>
                    <td className="px-5 py-3">
                      <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 font-mono text-[11px]">
                        {r.ruleType}
                      </span>
                    </td>
                    <td className={`px-5 py-3 font-bold ${r.creditScore >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                      {r.creditScore}
                    </td>
                    <td className="px-5 py-3 text-right space-x-2">
                      <button type="button" onClick={() => startEditStringRule(r.id, r.cityName, r.ruleType, r.creditScore)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-blue-600 cursor-pointer">
                        <Pencil size={14} />
                      </button>
                      <button type="button" onClick={() => handleDelete(r.id)} className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-rose-600 cursor-pointer">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}

              {((category === 'age' && ageRules.length === 0) ||
                (category === 'gender' && genderRules.length === 0) ||
                (category === 'country' && countryRules.length === 0) ||
                (category === 'city' && cityRules.length === 0)) && (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-slate-400">
                    {loading
                      ? isRu ? 'Загрузка...' : 'Loading...'
                      : isRu ? 'Правила не найдены' : 'No rules found'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
